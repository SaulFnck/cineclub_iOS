package com.sauldiaz.cineclub_api.util;

import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/** Construye las respuestas HTTP en JSON con UTF-8 (para acentos y ñ). */
public final class Respuesta {

    public static final String JSON = "application/json; charset=UTF-8";

    private Respuesta() {
    }

    public static Response ok(Object cuerpo) {
        return Response.ok(GsonUtil.GSON.toJson(cuerpo), JSON).build();
    }

    public static Response creado(Object cuerpo) {
        return Response.status(201).entity(GsonUtil.GSON.toJson(cuerpo)).type(JSON).build();
    }

    public static Response sinContenido() {
        return Response.noContent().build();
    }

    public static Response error(int status, String mensaje, Map<String, Object> extra) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje);
        if (extra != null) {
            cuerpo.putAll(extra);
        }
        return Response.status(status).entity(GsonUtil.GSON.toJson(cuerpo)).type(JSON).build();
    }
}
