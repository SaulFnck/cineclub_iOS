package com.sauldiaz.cineclub_api.dao;

import com.sauldiaz.cineclub_api.modelo.Pelicula;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla peliculas (incluye promedio de rating y total de reseñas). */
public class PeliculaDAO {

    private static final String SELECT_BASE =
            "SELECT p.*, ROUND(AVG(r.rating), 1) AS rating_promedio, COUNT(r.id_resenia) AS total_resenias "
            + "FROM peliculas p LEFT JOIN resenias r ON r.id_pelicula = p.id_pelicula ";
    private static final String GROUP = " GROUP BY p.id_pelicula ";

    /** Todos los filtros son opcionales (null = sin filtro). */
    public List<Pelicula> listar(String titulo, String genero, String director,
                                 boolean soloDisponibles, String orden) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (titulo != null && !titulo.isBlank()) {
            sql.append("AND p.titulo LIKE ? ");
            params.add("%" + titulo.trim() + "%");
        }
        if (genero != null && !genero.isBlank()) {
            sql.append("AND p.genero = ? ");
            params.add(genero.trim());
        }
        if (director != null && !director.isBlank()) {
            sql.append("AND p.director LIKE ? ");
            params.add("%" + director.trim() + "%");
        }
        if (soloDisponibles) {
            sql.append("AND p.stock_disponible > 0 ");
        }
        sql.append(GROUP).append("ORDER BY ").append(columnaOrden(orden));

        return consultar(sql.toString(), params.toArray());
    }

    public Pelicula buscarPorId(int id) throws SQLException {
        List<Pelicula> r = consultar(SELECT_BASE + "WHERE p.id_pelicula = ?" + GROUP, id);
        return r.isEmpty() ? null : r.get(0);
    }

    public boolean existe(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM peliculas WHERE id_pelicula = ?", id) > 0;
    }

    public List<String> generos() throws SQLException {
        List<String> lista = new ArrayList<>();
        try (Connection con = DAOUtil.conectar();
             PreparedStatement ps = con.prepareStatement("SELECT DISTINCT genero FROM peliculas ORDER BY genero");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(rs.getString(1));
            }
        }
        return lista;
    }

    public int insertar(Pelicula p) throws SQLException {
        return DAOUtil.insertar(
                "INSERT INTO peliculas (titulo, genero, director, fecha_estreno, costo_renta, stock_disponible, imagen_archivo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                p.getTitulo(), p.getGenero(), p.getDirector(), p.getFechaEstreno(),
                p.getCostoRenta(), p.getStockDisponible(), p.getImagenArchivo());
    }

    public void actualizar(Pelicula p) throws SQLException {
        DAOUtil.actualizar(
                "UPDATE peliculas SET titulo = ?, genero = ?, director = ?, fecha_estreno = ?, costo_renta = ?, "
                + "stock_disponible = ?, imagen_archivo = ? WHERE id_pelicula = ?",
                p.getTitulo(), p.getGenero(), p.getDirector(), p.getFechaEstreno(),
                p.getCostoRenta(), p.getStockDisponible(), p.getImagenArchivo(), p.getIdPelicula());
    }

    /**
     * Suma (o resta, si es negativa) la cantidad al stock en una sola sentencia atómica.
     * @return false si el resultado quedaría por debajo de 0 (no se modifica nada).
     */
    public boolean ajustarStock(int id, int cantidad) throws SQLException {
        return DAOUtil.actualizar(
                "UPDATE peliculas SET stock_disponible = stock_disponible + ? "
                + "WHERE id_pelicula = ? AND stock_disponible + ? >= 0",
                cantidad, id, cantidad) > 0;
    }

    public int contarRentas(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM rentas WHERE id_pelicula = ?", id);
    }

    public int contarResenias(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM resenias WHERE id_pelicula = ?", id);
    }

    public void eliminar(int id) throws SQLException {
        DAOUtil.actualizar("DELETE FROM peliculas WHERE id_pelicula = ?", id);
    }

    // ---------- Privados ----------

    /** Lista blanca de columnas para ORDER BY (evita inyección SQL). */
    private String columnaOrden(String orden) {
        if (orden == null) {
            return "p.titulo";
        }
        return switch (orden.toLowerCase()) {
            case "estreno" -> "p.fecha_estreno DESC";
            case "rating" -> "rating_promedio DESC, p.titulo";
            case "precio" -> "p.costo_renta";
            default -> "p.titulo";
        };
    }

    private List<Pelicula> consultar(String sql, Object... params) throws SQLException {
        List<Pelicula> lista = new ArrayList<>();
        try (Connection con = DAOUtil.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            DAOUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private Pelicula mapear(ResultSet rs) throws SQLException {
        Pelicula p = new Pelicula();
        p.setIdPelicula(rs.getInt("id_pelicula"));
        p.setTitulo(rs.getString("titulo"));
        p.setGenero(rs.getString("genero"));
        p.setDirector(rs.getString("director"));
        p.setFechaEstreno(rs.getObject("fecha_estreno", LocalDate.class));
        p.setCostoRenta(rs.getBigDecimal("costo_renta"));
        p.setStockDisponible(rs.getInt("stock_disponible"));
        p.setImagenArchivo(rs.getString("imagen_archivo"));
        BigDecimal promedio = rs.getBigDecimal("rating_promedio");
        p.setRatingPromedio(promedio == null ? null : promedio.doubleValue());
        p.setTotalResenias(rs.getInt("total_resenias"));
        return p;
    }
}
