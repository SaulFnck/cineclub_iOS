package com.sauldiaz.cineclub_api.resources;

import com.sauldiaz.cineclub_api.dao.EmpleadoDAO;
import com.sauldiaz.cineclub_api.dao.RentaDAO;
import com.sauldiaz.cineclub_api.dao.UsuarioDAO;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Renta;
import com.sauldiaz.cineclub_api.util.GsonUtil;
import com.sauldiaz.cineclub_api.util.Respuesta;
import com.sauldiaz.cineclub_api.util.Validacion;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
 * Rentas (vista de empleado).
 * Crear → stock -1. Devolver → stock +1 y recargo de $25 por día de retraso.
 */
@Path("rentas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RentaResource {

    private final RentaDAO dao = new RentaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    /** GET /rentas?estado=activas|devueltas|vencidas&idUsuario= */
    @GET
    public Response listar(@QueryParam("estado") String estado,
                           @QueryParam("idUsuario") Integer idUsuario) throws SQLException {
        return Respuesta.ok(dao.listar(estado, idUsuario));
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") int id) throws SQLException {
        return Respuesta.ok(buscarOFallar(id));
    }

    /** POST /rentas  { idPelicula, idUsuario, idEmpleado } */
    @POST
    public Response crear(String cuerpo) throws SQLException {
        Renta r = GsonUtil.leer(cuerpo, Renta.class);
        int idPelicula = Validacion.requerido(r.getIdPelicula(), "idPelicula");
        int idUsuario = Validacion.requerido(r.getIdUsuario(), "idUsuario");
        int idEmpleado = Validacion.requerido(r.getIdEmpleado(), "idEmpleado");

        if (usuarioDAO.buscarPorId(idUsuario) == null) {
            throw ApiException.notFound("El usuario " + idUsuario + " no existe");
        }
        if (empleadoDAO.buscarPorId(idEmpleado) == null) {
            throw ApiException.notFound("El empleado " + idEmpleado + " no existe");
        }
        // La existencia de la película y el stock se validan dentro de la transacción
        int id = dao.crear(idPelicula, idUsuario, idEmpleado);
        return Respuesta.creado(dao.buscarPorId(id));
    }

    /** PUT /rentas/{id}  { "fechaLimiteDevolucion": "2026-11-25T18:00:00" } — prórroga. */
    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") int id, String cuerpo) throws SQLException {
        Renta cambios = GsonUtil.leer(cuerpo, Renta.class);
        dao.actualizarFechaLimite(id,
                Validacion.requerido(cambios.getFechaLimiteDevolucion(), "fechaLimiteDevolucion"));
        return Respuesta.ok(dao.buscarPorId(id));
    }

    /** PATCH /rentas/{id}/devolucion — el usuario entregó la película. */
    @PATCH
    @Path("{id}/devolucion")
    public Response devolver(@PathParam("id") int id) throws SQLException {
        dao.registrarDevolucion(id);
        return Respuesta.ok(dao.buscarPorId(id));
    }

    /** PATCH /rentas/{id}/cancelar-devolucion — revertir una devolución marcada por error. */
    @PATCH
    @Path("{id}/cancelar-devolucion")
    public Response cancelarDevolucion(@PathParam("id") int id) throws SQLException {
        dao.cancelarDevolucion(id);
        return Respuesta.ok(dao.buscarPorId(id));
    }

    /** DELETE /rentas/{id} — solo para rentas registradas por error. */
    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id) throws SQLException {
        dao.eliminar(id);
        return Respuesta.sinContenido();
    }

    private Renta buscarOFallar(int id) throws SQLException {
        Renta r = dao.buscarPorId(id);
        if (r == null) {
            throw ApiException.notFound("La renta " + id + " no existe");
        }
        return r;
    }
}
