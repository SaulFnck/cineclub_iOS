package com.sauldiaz.cineclub_api.modelo;

/** Tabla empleados. El password solo se recibe; nunca se devuelve en el JSON. */
public class Empleado {

    private Integer idEmpleado;
    private String nombreCompleto;
    private String correo;
    private String password;

    public Empleado() {
    }

    public Empleado(Integer idEmpleado, String nombreCompleto, String correo) {
        this.idEmpleado = idEmpleado;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
    }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
