package com.sauldiaz.cineclub_api.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sauldiaz.cineclub_api.excepciones.ApiException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson configurado para la app SwiftUI:
 *  - DATE      -> "2008-03-14"
 *  - DATETIME  -> "2026-11-12T18:30:00"  (ISO-8601, decodificable con Codable)
 * Los campos null se omiten; en Swift decláralos como opcionales.
 */
public final class GsonUtil {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate v) throws IOException {
                    out.value(v.toString());
                }

                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    return LocalDate.parse(in.nextString());
                }
            }.nullSafe())
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime v) throws IOException {
                    out.value(v.format(FECHA_HORA));
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    return LocalDateTime.parse(in.nextString());
                }
            }.nullSafe())
            .create();

    private GsonUtil() {
    }

    /** Convierte el cuerpo de la petición; 400 si viene vacío o mal formado. */
    public static <T> T leer(String cuerpo, Class<T> clase) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw ApiException.badRequest("El cuerpo de la petición está vacío");
        }
        try {
            T obj = GSON.fromJson(cuerpo, clase);
            if (obj == null) {
                throw ApiException.badRequest("El cuerpo de la petición está vacío");
            }
            return obj;
        } catch (JsonParseException | java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("JSON inválido: " + e.getMessage());
        }
    }
}
