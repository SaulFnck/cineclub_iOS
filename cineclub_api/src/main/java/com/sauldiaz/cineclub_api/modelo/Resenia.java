package com.sauldiaz.cineclub_api.modelo;

import java.time.LocalDateTime;

/** Tabla resenias + datos de la película y del autor (solo lectura). */
public class Resenia {

    private Integer idResenia;
    private Integer idPelicula;
    private Integer idUsuario;
    private String comentario;
    private Integer rating;
    private LocalDateTime fechaCreacion;

    // Datos relacionados (JOIN)
    private String tituloPelicula;
    private String nombreUsuario;

    public Integer getIdResenia() { return idResenia; }
    public void setIdResenia(Integer idResenia) { this.idResenia = idResenia; }

    public Integer getIdPelicula() { return idPelicula; }
    public void setIdPelicula(Integer idPelicula) { this.idPelicula = idPelicula; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getTituloPelicula() { return tituloPelicula; }
    public void setTituloPelicula(String tituloPelicula) { this.tituloPelicula = tituloPelicula; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
}
