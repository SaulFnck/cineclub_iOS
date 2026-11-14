package com.sauldiaz.cineclub_api.resources;

import com.sauldiaz.cineclub_api.dao.EmpleadoDAO;
import com.sauldiaz.cineclub_api.dao.UsuarioDAO;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Empleado;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** CRUD de empleados (solo para la vista de empleado en la app). */
@Path("empleados")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EmpleadoResource {

    private final EmpleadoDAO dao = new EmpleadoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @GET
    public Response listar() throws SQLException {
        return Respuesta.ok(dao.listar());
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") int id) throws SQLException {
        return Respuesta.ok(buscarOFallar(id));
    }

    @POST
    public Response crear(String cuerpo) throws SQLException {
        Empleado e = GsonUtil.leer(cuerpo, Empleado.class);
        validar(e, true);
        verificarCorreoLibre(e.getCorreo(), null);
        e.setIdEmpleado(dao.insertar(e));
        e.setPassword(null);
        return Respuesta.creado(e);
    }

    @PUT
    @Path("{id}")
    public Response actualizar(@PathParam("id") int id, String cuerpo) throws SQLException {
        buscarOFallar(id);
        Empleado e = GsonUtil.leer(cuerpo, Empleado.class);
        validar(e, false);
        verificarCorreoLibre(e.getCorreo(), id);
        e.setIdEmpleado(id);
        dao.actualizar(e);
        return Respuesta.ok(dao.buscarPorId(id));
    }

    @DELETE
    @Path("{id}")
    public Response eliminar(@PathParam("id") int id) throws SQLException {
        buscarOFallar(id);
        int rentas = dao.contarRentas(id);
        if (rentas > 0) {
            Map<String, Integer> deps = new LinkedHashMap<>();
            deps.put("rentas", rentas);
            throw ApiException.conflict("No se puede eliminar el empleado porque registró rentas",
                    deps, "Conserva el registro para mantener el historial de rentas");
        }
        dao.eliminar(id);
        return Respuesta.sinContenido();
    }

    // ---------- Privados ----------

    private Empleado buscarOFallar(int id) throws SQLException {
        Empleado e = dao.buscarPorId(id);
        if (e == null) {
            throw ApiException.notFound("El empleado " + id + " no existe");
        }
        return e;
    }

    /** En actualización el password es opcional (si no viene, no se cambia). */
    private void validar(Empleado e, boolean passwordObligatorio) {
        e.setNombreCompleto(Validacion.texto(e.getNombreCompleto(), "nombreCompleto", 150));
        e.setCorreo(Validacion.correo(e.getCorreo()));
        e.setPassword(passwordObligatorio
                ? Validacion.texto(e.getPassword(), "password", 255)
                : Validacion.textoOpcional(e.getPassword(), "password", 255));
    }

    /** El correo debe ser único entre empleados Y usuarios (el login busca en ambas tablas). */
    private void verificarCorreoLibre(String correo, Integer idActual) throws SQLException {
        if (dao.existeCorreo(correo, idActual) || usuarioDAO.existeCorreo(correo, null)) {
            throw ApiException.conflict("El correo ya está registrado");
        }
    }
}
