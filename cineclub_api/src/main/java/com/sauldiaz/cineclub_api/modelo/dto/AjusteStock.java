package com.sauldiaz.cineclub_api.modelo.dto;

/** Cuerpo de PATCH /peliculas/{id}/stock. Ej: { "cantidad": 5 } o { "cantidad": -2 } */
public class AjusteStock {

    private Integer cantidad;

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
