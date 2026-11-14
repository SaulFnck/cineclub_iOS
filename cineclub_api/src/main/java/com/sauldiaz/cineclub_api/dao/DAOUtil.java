package com.sauldiaz.cineclub_api.dao;

import com.sauldiaz.cineclub_api.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Utilidades comunes de acceso a datos. */
final class DAOUtil {

    private DAOUtil() {
    }

    static Connection conectar() throws SQLException {
        return ConexionBD.getInstancia().getConexion();
    }

    /** Asigna parámetros posicionales (?, ?, ...) a un PreparedStatement. */
    static void parametros(PreparedStatement ps, Object... valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) {
            ps.setObject(i + 1, valores[i]);
        }
    }

    /** Ejecuta un SELECT COUNT(*) y devuelve el número. */
    static int contar(String sql, Object... valores) throws SQLException {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            parametros(ps, valores);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Ejecuta INSERT/UPDATE/DELETE y devuelve filas afectadas. */
    static int actualizar(String sql, Object... valores) throws SQLException {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            parametros(ps, valores);
            return ps.executeUpdate();
        }
    }

    /** Ejecuta un INSERT y devuelve el id autogenerado. */
    static int insertar(String sql, Object... valores) throws SQLException {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            parametros(ps, valores);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
