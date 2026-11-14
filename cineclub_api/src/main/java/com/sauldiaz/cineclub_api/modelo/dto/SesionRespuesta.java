package com.sauldiaz.cineclub_api.modelo.dto;

/**
 * Respuesta de login/registro. La app SwiftUI usa "rol" para mostrar u ocultar opciones.
 */
public class SesionRespuesta {

    public static final String ROL_EMPLEADO = "EMPLEADO";
    public static final String ROL_USUARIO = "USUARIO";

    private String rol;
    private Integer id;
    private String nombreCompleto;
    private String correo;

    public SesionRespuesta(String rol, Integer id, String nombreCompleto, String correo) {
        this.rol = rol;
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
    }

    public String getRol() { return rol; }
    public Integer getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCorreo() { return correo; }
}
