package com.sauldiaz.cineclub_api.util;

import com.sauldiaz.cineclub_api.excepciones.ApiException;

/** Validaciones de entrada; lanzan 400 con un mensaje claro. */
public final class Validacion {

    private Validacion() {
    }

    /** Texto obligatorio: lo recorta y valida longitud máxima. */
    public static String texto(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw ApiException.badRequest("El campo '" + campo + "' es obligatorio");
        }
        return textoOpcional(valor, campo, max);
    }

    /** Texto opcional: null si viene vacío. */
    public static String textoOpcional(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw ApiException.badRequest("El campo '" + campo + "' admite máximo " + max + " caracteres");
        }
        return limpio;
    }

    public static String correo(String valor) {
        String c = texto(valor, "correo", 100).toLowerCase();
        if (!c.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.badRequest("El correo no tiene un formato válido");
        }
        return c;
    }

    public static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw ApiException.badRequest("El campo '" + campo + "' es obligatorio");
        }
        return valor;
    }

    public static int rating(Integer valor) {
        requerido(valor, "rating");
        if (valor < 1 || valor > 5) {
            throw ApiException.badRequest("El rating debe estar entre 1 y 5");
        }
        return valor;
    }
}
