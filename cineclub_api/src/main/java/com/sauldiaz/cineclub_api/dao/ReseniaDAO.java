package com.sauldiaz.cineclub_api.dao;

import com.sauldiaz.cineclub_api.modelo.Resenia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla resenias (con título de película y nombre del autor). */
public class ReseniaDAO {

    private static final String SELECT_BASE =
            "SELECT r.*, p.titulo, u.nombre_completo FROM resenias r "
            + "JOIN peliculas p ON p.id_pelicula = r.id_pelicula "
            + "JOIN usuarios u ON u.id_usuario = r.id_usuario ";
    private static final String ORDEN = " ORDER BY r.fecha_creacion DESC";

    /** Filtros opcionales (null = sin filtro). */
    public List<Resenia> listar(Integer idPelicula, Integer idUsuario, Integer rating) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (idPelicula != null) {
            sql.append("AND r.id_pelicula = ? ");
            params.add(idPelicula);
        }
        if (idUsuario != null) {
            sql.append("AND r.id_usuario = ? ");
            params.add(idUsuario);
        }
        if (rating != null) {
            sql.append("AND r.rating = ? ");
            params.add(rating);
        }
        return consultar(sql.append(ORDEN).toString(), params.toArray());
    }

    public Resenia buscarPorId(int id) throws SQLException {
        List<Resenia> r = consultar(SELECT_BASE + "WHERE r.id_resenia = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    public int insertar(Resenia r) throws SQLException {
        return DAOUtil.insertar(
                "INSERT INTO resenias (id_pelicula, id_usuario, comentario, rating) VALUES (?, ?, ?, ?)",
                r.getIdPelicula(), r.getIdUsuario(), r.getComentario(), r.getRating());
    }

    public void actualizar(int id, String comentario, int rating) throws SQLException {
        DAOUtil.actualizar("UPDATE resenias SET comentario = ?, rating = ? WHERE id_resenia = ?",
                comentario, rating, id);
    }

    public void eliminar(int id) throws SQLException {
        DAOUtil.actualizar("DELETE FROM resenias WHERE id_resenia = ?", id);
    }

    // ---------- Privados ----------

    private List<Resenia> consultar(String sql, Object... params) throws SQLException {
        List<Resenia> lista = new ArrayList<>();
        try (Connection con = DAOUtil.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            DAOUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Resenia r = new Resenia();
                    r.setIdResenia(rs.getInt("id_resenia"));
                    r.setIdPelicula(rs.getInt("id_pelicula"));
                    r.setIdUsuario(rs.getInt("id_usuario"));
                    r.setComentario(rs.getString("comentario"));
                    r.setRating(rs.getInt("rating"));
                    r.setFechaCreacion(rs.getObject("fecha_creacion", LocalDateTime.class));
                    r.setTituloPelicula(rs.getString("titulo"));
                    r.setNombreUsuario(rs.getString("nombre_completo"));
                    lista.add(r);
                }
            }
        }
        return lista;
    }
}
