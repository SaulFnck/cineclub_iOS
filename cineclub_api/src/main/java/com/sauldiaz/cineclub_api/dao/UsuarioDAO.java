package com.sauldiaz.cineclub_api.dao;

import com.sauldiaz.cineclub_api.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla usuarios. */
public class UsuarioDAO {

    private static final String COLUMNAS = "SELECT id_usuario, nombre_completo, correo_usuario FROM usuarios ";

    /** nombre: filtro parcial opcional. */
    public List<Usuario> listar(String nombre) throws SQLException {
        if (nombre == null || nombre.isBlank()) {
            return consultar(COLUMNAS + "ORDER BY nombre_completo");
        }
        return consultar(COLUMNAS + "WHERE nombre_completo LIKE ? ORDER BY nombre_completo", "%" + nombre.trim() + "%");
    }

    public Usuario buscarPorId(int id) throws SQLException {
        List<Usuario> r = consultar(COLUMNAS + "WHERE id_usuario = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    public Usuario login(String correo, String password) throws SQLException {
        List<Usuario> r = consultar(COLUMNAS + "WHERE correo_usuario = ? AND password_usuario = ?", correo, password);
        return r.isEmpty() ? null : r.get(0);
    }

    public boolean existeCorreo(String correo, Integer excluirId) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM usuarios WHERE correo_usuario = ? AND id_usuario <> ?",
                correo, excluirId == null ? 0 : excluirId) > 0;
    }

    public int insertar(Usuario u) throws SQLException {
        return DAOUtil.insertar(
                "INSERT INTO usuarios (nombre_completo, correo_usuario, password_usuario) VALUES (?, ?, ?)",
                u.getNombreCompleto(), u.getCorreo(), u.getPassword());
    }

    /** Si password viene null, no se modifica. */
    public void actualizar(Usuario u) throws SQLException {
        if (u.getPassword() == null) {
            DAOUtil.actualizar("UPDATE usuarios SET nombre_completo = ?, correo_usuario = ? WHERE id_usuario = ?",
                    u.getNombreCompleto(), u.getCorreo(), u.getIdUsuario());
        } else {
            DAOUtil.actualizar("UPDATE usuarios SET nombre_completo = ?, correo_usuario = ?, password_usuario = ? WHERE id_usuario = ?",
                    u.getNombreCompleto(), u.getCorreo(), u.getPassword(), u.getIdUsuario());
        }
    }

    public int contarRentasActivas(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM rentas WHERE id_usuario = ? AND fecha_devolucion_real IS NULL", id);
    }

    public int contarRentas(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM rentas WHERE id_usuario = ?", id);
    }

    public int contarResenias(int id) throws SQLException {
        return DAOUtil.contar("SELECT COUNT(*) FROM resenias WHERE id_usuario = ?", id);
    }

    public void eliminar(int id) throws SQLException {
        DAOUtil.actualizar("DELETE FROM usuarios WHERE id_usuario = ?", id);
    }

    // ---------- Privados ----------

    private List<Usuario> consultar(String sql, Object... params) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = DAOUtil.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            DAOUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Usuario(rs.getInt("id_usuario"),
                            rs.getString("nombre_completo"),
                            rs.getString("correo_usuario")));
                }
            }
        }
        return lista;
    }
}
