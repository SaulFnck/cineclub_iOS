package com.sauldiaz.cineclub_api.dao;

import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Renta;
import com.sauldiaz.cineclub_api.util.ReglasRenta;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla rentas.
 * Las operaciones que tocan el inventario (crear, devolver, cancelar devolución,
 * eliminar) corren en UNA transacción: renta y stock cambian juntos o no cambian.
 */
public class RentaDAO {

    private static final String SELECT_BASE =
            "SELECT r.*, p.titulo, u.nombre_completo AS nombre_usuario, e.nombre_completo AS nombre_empleado "
            + "FROM rentas r "
            + "JOIN peliculas p ON p.id_pelicula = r.id_pelicula "
            + "JOIN usuarios u ON u.id_usuario = r.id_usuario "
            + "JOIN empleados e ON e.id_empleado = r.id_empleado ";

    // =====================================================================
    // Consultas
    // =====================================================================

    /** estado: activas | devueltas | vencidas | null (todas). idUsuario opcional. */
    public List<Renta> listar(String estado, Integer idUsuario) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (estado != null && !estado.isBlank()) {
            switch (estado.toLowerCase()) {
                case "activas" -> sql.append("AND r.fecha_devolucion_real IS NULL ");
                case "devueltas" -> sql.append("AND r.fecha_devolucion_real IS NOT NULL ");
                case "vencidas" -> sql.append("AND r.fecha_devolucion_real IS NULL "
                        + "AND DATE(r.fecha_limite_devolucion) < CURDATE() ");
                default -> throw ApiException.badRequest("Estado inválido. Usa: activas, devueltas o vencidas");
            }
        }
        if (idUsuario != null) {
            sql.append("AND r.id_usuario = ? ");
            params.add(idUsuario);
        }
        sql.append("ORDER BY r.fecha_prestamo DESC");
        return consultar(sql.toString(), params.toArray());
    }

    public Renta buscarPorId(int id) throws SQLException {
        List<Renta> r = consultar(SELECT_BASE + "WHERE r.id_renta = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    // =====================================================================
    // Operaciones con inventario (transaccionales)
    // =====================================================================

    /** Registra el préstamo: valida stock, resta 1 y crea la renta con plazo de 7 días. */
    public int crear(int idPelicula, int idUsuario, int idEmpleado) throws SQLException {
        try (Connection con = DAOUtil.conectar()) {
            con.setAutoCommit(false);
            try {
                // 1. Bloquea la fila de la película (evita rentar la última copia dos veces)
                BigDecimal precio;
                int stock;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT stock_disponible, costo_renta FROM peliculas WHERE id_pelicula = ? FOR UPDATE")) {
                    ps.setInt(1, idPelicula);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw ApiException.notFound("La película " + idPelicula + " no existe");
                        }
                        stock = rs.getInt("stock_disponible");
                        precio = rs.getBigDecimal("costo_renta");
                    }
                }
                if (stock <= 0) {
                    throw ApiException.conflict("Sin stock disponible para esta película");
                }

                // 2. Stock -1
                ejecutar(con, "UPDATE peliculas SET stock_disponible = stock_disponible - 1 WHERE id_pelicula = ?", idPelicula);

                // 3. Crea la renta (precio base = costo_renta actual de la película)
                LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
                int idRenta;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO rentas (id_pelicula, id_usuario, id_empleado, fecha_prestamo, "
                        + "fecha_limite_devolucion, monto_cobrado) VALUES (?, ?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    DAOUtil.parametros(ps, idPelicula, idUsuario, idEmpleado, ahora,
                            ahora.plusDays(ReglasRenta.DIAS_PLAZO), precio);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idRenta = rs.getInt(1);
                    }
                }

                con.commit();
                return idRenta;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Marca la renta como devuelta: aplica recargo por retraso y suma 1 al stock. */
    public void registrarDevolucion(int idRenta) throws SQLException {
        try (Connection con = DAOUtil.conectar()) {
            con.setAutoCommit(false);
            try {
                Renta r = bloquear(con, idRenta);
                if (r.getFechaDevolucionReal() != null) {
                    throw ApiException.conflict("La renta ya fue devuelta");
                }

                LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
                int dias = ReglasRenta.diasRetraso(r.getFechaLimiteDevolucion(), ahora);
                BigDecimal total = r.getMontoCobrado().add(ReglasRenta.recargo(dias));

                ejecutar(con, "UPDATE rentas SET fecha_devolucion_real = ?, monto_cobrado = ? WHERE id_renta = ?",
                        ahora, total, idRenta);
                // Stock +1
                ejecutar(con, "UPDATE peliculas SET stock_disponible = stock_disponible + 1 WHERE id_pelicula = ?",
                        r.getIdPelicula());

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Revierte una devolución marcada por error: quita el recargo y resta 1 al stock. */
    public void cancelarDevolucion(int idRenta) throws SQLException {
        try (Connection con = DAOUtil.conectar()) {
            con.setAutoCommit(false);
            try {
                Renta r = bloquear(con, idRenta);
                if (r.getFechaDevolucionReal() == null) {
                    throw ApiException.conflict("La renta no ha sido devuelta");
                }

                int dias = ReglasRenta.diasRetraso(r.getFechaLimiteDevolucion(), r.getFechaDevolucionReal());
                BigDecimal precioBase = r.getMontoCobrado().subtract(ReglasRenta.recargo(dias));

                // Stock -1 (solo si hay copias; si no, alguien más ya la rentó)
                int filas = ejecutar(con, "UPDATE peliculas SET stock_disponible = stock_disponible - 1 "
                        + "WHERE id_pelicula = ? AND stock_disponible > 0", r.getIdPelicula());
                if (filas == 0) {
                    throw ApiException.conflict("No hay stock para revertir la devolución; ajusta el inventario primero");
                }
                ejecutar(con, "UPDATE rentas SET fecha_devolucion_real = NULL, monto_cobrado = ? WHERE id_renta = ?",
                        precioBase, idRenta);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Prórroga: cambia la fecha límite de una renta que sigue activa. */
    public void actualizarFechaLimite(int idRenta, LocalDateTime nuevaFecha) throws SQLException {
        Renta r = buscarPorId(idRenta);
        if (r == null) {
            throw ApiException.notFound("La renta " + idRenta + " no existe");
        }
        if (r.getFechaDevolucionReal() != null) {
            throw ApiException.conflict("No se puede modificar una renta ya devuelta");
        }
        if (!nuevaFecha.isAfter(r.getFechaPrestamo())) {
            throw ApiException.badRequest("La fecha límite debe ser posterior a la fecha de préstamo");
        }
        DAOUtil.actualizar("UPDATE rentas SET fecha_limite_devolucion = ? WHERE id_renta = ?", nuevaFecha, idRenta);
    }

    /** Elimina una renta registrada por error. Si seguía activa, regresa la copia al stock. */
    public void eliminar(int idRenta) throws SQLException {
        try (Connection con = DAOUtil.conectar()) {
            con.setAutoCommit(false);
            try {
                Renta r = bloquear(con, idRenta);
                if (r.getFechaDevolucionReal() == null) {
                    ejecutar(con, "UPDATE peliculas SET stock_disponible = stock_disponible + 1 WHERE id_pelicula = ?",
                            r.getIdPelicula());
                }
                ejecutar(con, "DELETE FROM rentas WHERE id_renta = ?", idRenta);
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    // =====================================================================
    // Privados
    // =====================================================================

    /** Lee la renta con FOR UPDATE dentro de la transacción; 404 si no existe. */
    private Renta bloquear(Connection con, int idRenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rentas WHERE id_renta = ? FOR UPDATE")) {
            ps.setInt(1, idRenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw ApiException.notFound("La renta " + idRenta + " no existe");
                }
                return mapearBasico(rs);
            }
        }
    }

    private int ejecutar(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            DAOUtil.parametros(ps, params);
            return ps.executeUpdate();
        }
    }

    private List<Renta> consultar(String sql, Object... params) throws SQLException {
        List<Renta> lista = new ArrayList<>();
        try (Connection con = DAOUtil.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            DAOUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Renta r = mapearBasico(rs);
                    r.setTituloPelicula(rs.getString("titulo"));
                    r.setNombreUsuario(rs.getString("nombre_usuario"));
                    r.setNombreEmpleado(rs.getString("nombre_empleado"));
                    ReglasRenta.calcularCampos(r);
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    private Renta mapearBasico(ResultSet rs) throws SQLException {
        Renta r = new Renta();
        r.setIdRenta(rs.getInt("id_renta"));
        r.setIdPelicula(rs.getInt("id_pelicula"));
        r.setIdUsuario(rs.getInt("id_usuario"));
        r.setIdEmpleado(rs.getInt("id_empleado"));
        r.setFechaPrestamo(rs.getObject("fecha_prestamo", LocalDateTime.class));
        r.setFechaLimiteDevolucion(rs.getObject("fecha_limite_devolucion", LocalDateTime.class));
        r.setFechaDevolucionReal(rs.getObject("fecha_devolucion_real", LocalDateTime.class));
        r.setMontoCobrado(rs.getBigDecimal("monto_cobrado"));
        return r;
    }
}
