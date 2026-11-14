package com.sauldiaz.cineclub_api.excepciones;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error controlado de la API. Lleva el código HTTP y, opcionalmente,
 * datos extra que se agregan al JSON de error (dependencias, sugerencia...).
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final Map<String, Object> extra;

    public ApiException(int status, String mensaje) {
        this(status, mensaje, null);
    }

    public ApiException(int status, String mensaje, Map<String, Object> extra) {
        super(mensaje);
        this.status = status;
        this.extra = extra;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    // ---------- Atajos ----------

    public static ApiException badRequest(String mensaje) {
        return new ApiException(400, mensaje);
    }

    public static ApiException unauthorized(String mensaje) {
        return new ApiException(401, mensaje);
    }

    public static ApiException forbidden(String mensaje) {
        return new ApiException(403, mensaje);
    }

    public static ApiException notFound(String mensaje) {
        return new ApiException(404, mensaje);
    }

    public static ApiException conflict(String mensaje) {
        return new ApiException(409, mensaje);
    }

    /** 409 para borrados rechazados, con el detalle de dependencias. */
    public static ApiException conflict(String mensaje, Map<String, Integer> dependencias, String sugerencia) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("dependencias", dependencias);
        extra.put("sugerencia", sugerencia);
        return new ApiException(409, mensaje, extra);
    }
}
