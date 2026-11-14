package com.sauldiaz.cineclub_api.resources;

import com.sauldiaz.cineclub_api.dao.EmpleadoDAO;
import com.sauldiaz.cineclub_api.dao.RentaDAO;
import com.sauldiaz.cineclub_api.dao.ReseniaDAO;
import com.sauldiaz.cineclub_api.dao.UsuarioDAO;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Usuario;
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
import java.util.LinkedHashMap;
import java.util.Map;

/** CRUD de usuarios + su historial de rentas y reseñas. */
@Path("usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    private final UsuarioDAO dao = new UsuarioDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final RentaDAO rentaDAO = new RentaDAO();
    private final ReseniaDAO reseniaDAO = new ReseniaDAO();

    /** GET /usuarios?nombre=luis */
    @GET
    public Response listar(@QueryParam("nombre") String nombre) throws SQLException {
        return Respuesta.ok(dao.listar(nombre));
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") int id) throws SQLException {
        return Respuesta.ok(buscarOFallar(id));
    }

    /** GET /usuarios/{id}/rentas?estado=activas  ("Mis rentas") */
    @GET
    @Path("{id}/rentas")
    public Response rentas(@PathParam("id") int id, @QueryParam("estado") String estado) throws SQLException {
        buscarOFallar(id);
        return Respuesta.ok(rentaDAO.listar(estado, id));
    }

    /** GET /usuarios/{id}/resenias  ("Mis reseñas") */
    @GET
    @Path("{id}/resenias")
    public Response resenias(@PathParam("id") int id) throws SQLException {
        buscarOFallar(id);
        return Respuesta.ok(reseniaDAO.listar(null, id, null));
    }

    /** Alta desde el mostrador (empleado). El auto-registro es POST /auth/registro. */
    @POST
    public Response crear(String cuerpo) throws SQLException {
        Usuario u = GsonUtil.leer(cuerpo, Usuario.class);
        validar(u, true);
        verificarCorreoLibre(u.getCorreo(), null);
        u.setIdUsuario(dao.insertar(u));
        u.setPassword(null);
        return Respuesta.creado(u);
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") int id, String cuerpo) throws SQLException {
        buscarOFallar(id);
        Usuario u = GsonUtil.leer(cuerpo, Usuario.class);
        validar(u, false);
        verificarCorreoLibre(u.getCorreo(), id);
        u.setIdUsuario(id);
        dao.actualizar(u);
        return Respuesta.ok(dao.buscarPorId(id));
    }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id) throws SQLException {
        buscarOFallar(id);

        int activas = dao.contarRentasActivas(id);
        if (activas > 0) {
            Map<String, Integer> deps = new LinkedHashMap<>();
            deps.put("rentasActivas", activas);
            throw ApiException.conflict("No se puede eliminar el usuario porque tiene películas sin devolver",
                    deps, "El usuario debe devolver sus películas primero");
        }
        int rentas = dao.contarRentas(id);
        int resenias = dao.contarResenias(id);
        if (rentas > 0 || resenias > 0) {
            Map<String, Integer> deps = new LinkedHashMap<>();
            deps.put("rentas", rentas);
            deps.put("resenias", resenias);
            throw ApiException.conflict("No se puede eliminar el usuario porque tiene registros asociados",
                    deps, "Conserva el registro para mantener el historial");
        }
        dao.eliminar(id);
        return Respuesta.sinContenido();
    }

    // ---------- Privados ----------

    private Usuario buscarOFallar(int id) throws SQLException {
        Usuario u = dao.buscarPorId(id);
        if (u == null) {
            throw ApiException.notFound("El usuario " + id + " no existe");
        }
        return u;
    }

    private void validar(Usuario u, boolean passwordObligatorio) {
        u.setNombreCompleto(Validacion.texto(u.getNombreCompleto(), "nombreCompleto", 150));
        u.setCorreo(Validacion.correo(u.getCorreo()));
        u.setPassword(passwordObligatorio
                ? Validacion.texto(u.getPassword(), "password", 255)
                : Validacion.textoOpcional(u.getPassword(), "password", 255));
    }

    private void verificarCorreoLibre(String correo, Integer idActual) throws SQLException {
        if (dao.existeCorreo(correo, idActual) || empleadoDAO.existeCorreo(correo, null)) {
            throw ApiException.conflict("El correo ya está registrado");
        }
    }
}
