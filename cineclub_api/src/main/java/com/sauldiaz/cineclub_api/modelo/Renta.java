package com.sauldiaz.cineclub_api.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tabla rentas + nombres relacionados y campos calculados (solo lectura). */
public class Renta {

    private Integer idRenta;
    private Integer idPelicula;
    private Integer idUsuario;
    private Integer idEmpleado;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaLimiteDevolucion;
    private LocalDateTime fechaDevolucionReal;
    private BigDecimal montoCobrado;

    // Datos relacionados (JOIN)
    private String tituloPelicula;
    private String nombreUsuario;
    private String nombreEmpleado;

    // Calculados (ver ReglasRenta)
    private String estado;
    private Integer diasRetraso;
    private BigDecimal recargo;
    private BigDecimal montoActual;

    public Integer getIdRenta() { return idRenta; }
    public void setIdRenta(Integer idRenta) { this.idRenta = idRenta; }

    public Integer getIdPelicula() { return idPelicula; }
    public void setIdPelicula(Integer idPelicula) { this.idPelicula = idPelicula; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public LocalDateTime getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }

    public LocalDateTime getFechaLimiteDevolucion() { return fechaLimiteDevolucion; }
    public void setFechaLimiteDevolucion(LocalDateTime f) { this.fechaLimiteDevolucion = f; }

    public LocalDateTime getFechaDevolucionReal() { return fechaDevolucionReal; }
    public void setFechaDevolucionReal(LocalDateTime f) { this.fechaDevolucionReal = f; }

    public BigDecimal getMontoCobrado() { return montoCobrado; }
    public void setMontoCobrado(BigDecimal montoCobrado) { this.montoCobrado = montoCobrado; }

    public String getTituloPelicula() { return tituloPelicula; }
    public void setTituloPelicula(String tituloPelicula) { this.tituloPelicula = tituloPelicula; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getDiasRetraso() { return diasRetraso; }
    public void setDiasRetraso(Integer diasRetraso) { this.diasRetraso = diasRetraso; }

    public BigDecimal getRecargo() { return recargo; }
    public void setRecargo(BigDecimal recargo) { this.recargo = recargo; }

    public BigDecimal getMontoActual() { return montoActual; }
    public void setMontoActual(BigDecimal montoActual) { this.montoActual = montoActual; }
}
