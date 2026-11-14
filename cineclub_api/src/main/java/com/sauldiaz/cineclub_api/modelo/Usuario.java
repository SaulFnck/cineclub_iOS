package com.sauldiaz.cineclub_api.modelo;

/** Tabla usuarios. El password solo se recibe; nunca se devuelve en el JSON. */
public class Usuario {

    private Integer idUsuario;
    private String nombreCompleto;
    private String correo;
    private String password;

    public Usuario() {
    }

    public Usuario(Integer idUsuario, String nombreCompleto, String correo) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
