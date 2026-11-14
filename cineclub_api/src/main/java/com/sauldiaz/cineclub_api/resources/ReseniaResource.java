package com.sauldiaz.cineclub_api.resources;

import com.sauldiaz.cineclub_api.dao.EmpleadoDAO;
import com.sauldiaz.cineclub_api.dao.PeliculaDAO;
import com.sauldiaz.cineclub_api.dao.ReseniaDAO;
import com.sauldiaz.cineclub_api.dao.UsuarioDAO;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Resenia;
import com.sauldiaz.cineclub_api.util.GsonUtil;
import com.sauldiaz.cineclub_api.util.Respuesta;
import com.sauldiaz.cineclub_api.util.Validacion;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;

/**
 * Reseñas.
 *  - Usuario: crea, edita y elimina SOLO las suyas.
 *  - Empleado: solo elimina (moderación), no edita.
 */
@Path("resenias")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReseniaResource {

    private final ReseniaDAO dao = new ReseniaDAO();
    private final PeliculaDAO peliculaDAO = new PeliculaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    /** GET /resenias?idPelicula=&rating= */
    @GET
    public Response listar(@QueryParam("idPelicula") Integer idPelicula,
                           @QueryParam("rating") Integer rating) throws SQLException {
        return Respuesta.ok(dao.listar(idPelicula, null, rating));
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") int id) throws SQLException {
        return Respuesta.ok(buscarOFallar(id));
    }

    /** POST /resenias  { idPelicula, idUsuario, comentario, rating } */
    @POST
    public Response crear(String cuerpo) throws SQLException {
        Resenia r = GsonUtil.leer(cuerpo, Resenia.class);
        int idPelicula = Validacion.requerido(r.getIdPelicula(), "idPelicula");
        int idUsuario = Validacion.requerido(r.getIdUsuario(), "idUsuario");
        r.setRating(Validacion.rating(r.getRating()));
        r.setComentario(Validacion.textoOpcional(r.getComentario(), "comentario", 5000));

        if (!peliculaDAO.existe(idPelicula)) {
            throw ApiException.notFound("La película " + idPelicula + " no existe");
        }
        if (usuarioDAO.buscarPorId(idUsuario) == null) {
            throw ApiException.notFound("El usuario " + idUsuario + " no existe");
        }
        int id = dao.insertar(r);
        return Respuesta.creado(dao.buscarPorId(id));
    }

    /** PUT /resenias/{id}  { idUsuario, comentario, rating } — solo el autor. */
    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") int id, String cuerpo) throws SQLException {
        Resenia actual = buscarOFallar(id);
        Resenia cambios = GsonUtil.leer(cuerpo, Resenia.class);
        int idUsuario = Validacion.requerido(cambios.getIdUsuario(), "idUsuario");
        if (actual.getIdUsuario() != idUsuario) {
            throw ApiException.forbidden("Solo el autor puede editar esta reseña");
        }
        int rating = Validacion.rating(cambios.getRating());
        String comentario = Validacion.textoOpcional(cambios.getComentario(), "comentario", 5000);
        dao.actualizar(id, comentario, rating);
        return Respuesta.ok(dao.buscarPorId(id));
    }

    /**
     * DELETE /resenias/{id}?idUsuario=7   (el autor)
     * DELETE /resenias/{id}?idEmpleado=2  (moderación)
     */
    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id,
                             @QueryParam("idUsuario") Integer idUsuario,
                             @QueryParam("idEmpleado") Integer idEmpleado) throws SQLException {
        Resenia actual = buscarOFallar(id);
        if (idEmpleado != null) {
            if (empleadoDAO.buscarPorId(idEmpleado) == null) {
                throw ApiException.forbidden("Empleado no válido");
            }
        } else if (idUsuario != null) {
            if (!actual.getIdUsuario().equals(idUsuario)) {
                throw ApiException.forbidden("Solo el autor puede eliminar esta reseña");
            }
        } else {
            throw ApiException.badRequest("Indica idUsuario o idEmpleado en la URL");
        }
        dao.eliminar(id);
        return Respuesta.sinContenido();
    }

    private Resenia buscarOFallar(int id) throws SQLException {
        Resenia r = dao.buscarPorId(id);
        if (r == null) {
            throw ApiException.notFound("La reseña " + id + " no existe");
        }
        return r;
    }
}
