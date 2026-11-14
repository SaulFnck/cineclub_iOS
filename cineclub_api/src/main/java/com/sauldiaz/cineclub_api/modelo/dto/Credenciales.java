package com.sauldiaz.cineclub_api.modelo.dto;

/** Cuerpo de POST /auth/login. */
public class Credenciales {

    private String correo;
    private String password;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
