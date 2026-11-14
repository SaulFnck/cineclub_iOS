package com.sauldiaz.cineclub_api.resources;

import com.sauldiaz.cineclub_api.dao.EmpleadoDAO;
import com.sauldiaz.cineclub_api.dao.UsuarioDAO;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import com.sauldiaz.cineclub_api.modelo.Empleado;
import com.sauldiaz.cineclub_api.modelo.Usuario;
import com.sauldiaz.cineclub_api.modelo.dto.Credenciales;
import com.sauldiaz.cineclub_api.modelo.dto.SesionRespuesta;
import com.sauldiaz.cineclub_api.util.GsonUtil;
import com.sauldiaz.cineclub_api.util.Respuesta;
import com.sauldiaz.cineclub_api.util.Validacion;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;

/**
 * Login único y registro de usuarios.
 * La app SwiftUI usa el campo "rol" para decidir qué opciones mostrar.
 */
@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /** POST /auth/login — busca primero en empleados y después en usuarios. */
    @POST
    @Path("login")
    public Response login(String cuerpo) throws SQLException {
        Credenciales c = GsonUtil.leer(cuerpo, Credenciales.class);
        String correo = Validacion.correo(c.getCorreo());
        String password = Validacion.texto(c.getPassword(), "password", 255);

        Empleado e = empleadoDAO.login(correo, password);
        if (e != null) {
            return Respuesta.ok(new SesionRespuesta(SesionRespuesta.ROL_EMPLEADO,
                    e.getIdEmpleado(), e.getNombreCompleto(), e.getCorreo()));
        }
        Usuario u = usuarioDAO.login(correo, password);
        if (u != null) {
            return Respuesta.ok(new SesionRespuesta(SesionRespuesta.ROL_USUARIO,
                    u.getIdUsuario(), u.getNombreCompleto(), u.getCorreo()));
        }
        throw ApiException.unauthorized("Correo o contraseña incorrectos");
    }

    /** POST /auth/registro — crea SIEMPRE una cuenta de usuario y devuelve la sesión. */
    @POST
    @Path("registro")
    public Response registro(String cuerpo) throws SQLException {
        Usuario u = GsonUtil.leer(cuerpo, Usuario.class);
        u.setNombreCompleto(Validacion.texto(u.getNombreCompleto(), "nombreCompleto", 150));
        u.setCorreo(Validacion.correo(u.getCorreo()));
        u.setPassword(Validacion.texto(u.getPassword(), "password", 255));

        if (usuarioDAO.existeCorreo(u.getCorreo(), null) || empleadoDAO.existeCorreo(u.getCorreo(), null)) {
            throw ApiException.conflict("El correo ya está registrado");
        }
        int id = usuarioDAO.insertar(u);
        return Respuesta.creado(new SesionRespuesta(SesionRespuesta.ROL_USUARIO,
                id, u.getNombreCompleto(), u.getCorreo()));
    }
}
