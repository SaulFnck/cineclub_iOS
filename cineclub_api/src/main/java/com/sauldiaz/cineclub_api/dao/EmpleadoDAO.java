package com.sauldiaz.cineclub_api.dao;

import com.sauldiaz.cineclub_api.modelo.Empleado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla empleados. */
public class EmpleadoDAO {

    private static final String COLUMNAS = "SELECT id_empleado, nombre_completo, correo_empleado FROM empleados ";

    public List<Empleado> listar() throws SQLException {
        return consultar(COLUMNAS + "ORDER BY nombre_completo");
    }

    public Empleado buscarPorId(int id) throws SQLException {
        List<Empleado> r = consultar(COLUMNAS + "WHERE id_empleado = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Devuelve el empleado si correo y contraseña coinciden; null si no. */
    public Empleado login(String correo, String password) throws SQLException {
        List<Empleado> r = consultar(COLUMNAS + "WHERE correo_empleado = ? AND password_empleado = ?", correo, password);
        return r.isEmpty() ? null : r.get(0);
    }

    /** true si el correo ya lo usa otro empleado (excluirId puede ser null). */
    public boolean existeCorreo(String correo, Integer excluirId) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM empleados WHERE correo_empleado = ? AND id_empleado <> ?",
                correo, excluirId == null ? 0 : excluirId) > 0;
    }

    public int insertar(Empleado e) throws SQLException {
        return DAOUtil.insertar(
                "INSERT INTO empleados (nombre_completo, correo_empleado, password_empleado) VALUES (?, ?, ?)",
                e.getNombreCompleto(), e.getCorreo(), e.getPassword());
    }

    /** Si password viene null, no se modifica. */
    public void actualizar(Empleado e) throws SQLException {
        if (e.getPassword() == null) {
            DAOUtil.actualizar("UPDATE empleados SET nombre_completo = ?, correo_empleado = ? WHERE id_empleado = ?",
                    e.getNombreCompleto(), e.getCorreo(), e.getIdEmpleado());
        } else {
            DAOUtil.actualizar("UPDATE empleados SET nombre_completo = ?, correo_empleado = ?, password_empleado = ? WHERE id_empleado = ?",
                    e.getNombreCompleto(), e.getCorreo(), e.getPassword(), e.getIdEmpleado());
        }
    }

    public int contarRentas(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM rentas WHERE id_empleado = ?", id);
    }

    public void eliminar(int id) throws SQLException {
        DAOUtil.actualizar("DELETE FROM empleados WHERE id_empleado = ?", id);
    }

    // ---------- Privados ----------

    private List<Empleado> consultar(String sql, Object... params) throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        try (Connection con = DAOUtil.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            DAOUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Empleado(rs.getInt("id_empleado"),
                            rs.getString("nombre_completo"),
                            rs.getString("correo_empleado")));
                }
            }
        }
        return lista;
    }
}
