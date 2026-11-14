package com.sauldiaz.cineclub_api.excepciones;

import com.google.gson.JsonParseException;
import com.sauldiaz.cineclub_api.util.Respuesta;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convierte cualquier excepción en una respuesta JSON { "error": ... }.
 * Así los resources no necesitan try/catch y la app siempre recibe JSON.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof ApiException api) {
            return Respuesta.error(api.getStatus(), api.getMessage(), api.getExtra());
        }
        if (e instanceof WebApplicationException web) {
            int status = web.getResponse().getStatus();
            String msg = switch (status) {
                case 404 -> "Ruta no encontrada";
                case 405 -> "Método HTTP no permitido en esta ruta";
                case 415 -> "Envía el cuerpo como application/json";
                default -> web.getMessage();
            };
            return Respuesta.error(status, msg, null);
        }
        if (e instanceof JsonParseException) {
            return Respuesta.error(400, "JSON inválido: " + e.getMessage(), null);
        }
        // Llave foránea / UNIQUE no prevista en las validaciones
        for (Throwable c = e; c != null; c = c.getCause()) {
            if (c instanceof SQLIntegrityConstraintViolationException) {
                return Respuesta.error(409, "Conflicto de integridad: " + c.getMessage(), null);
            }
        }
        LOG.log(Level.SEVERE, "Error no controlado", e);
        return Respuesta.error(500, "Error interno del servidor: " + e.getMessage(), null);
    }
}
