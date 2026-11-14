package com.sauldiaz.cineclub_api.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tabla peliculas + estadísticas de reseñas (solo lectura). */
public class Pelicula {

    private Integer idPelicula;
    private String titulo;
    private String genero;
    private String director;
    private LocalDate fechaEstreno;
    private BigDecimal costoRenta;
    private Integer stockDisponible;
    private String imagenArchivo;

    // Calculados (no se guardan)
    private Double ratingPromedio;
    private Integer totalResenias;

    public Integer getIdPelicula() { return idPelicula; }
    public void setIdPelicula(Integer idPelicula) { this.idPelicula = idPelicula; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public LocalDate getFechaEstreno() { return fechaEstreno; }
    public void setFechaEstreno(LocalDate fechaEstreno) { this.fechaEstreno = fechaEstreno; }

    public BigDecimal getCostoRenta() { return costoRenta; }
    public void setCostoRenta(BigDecimal costoRenta) { this.costoRenta = costoRenta; }

    public Integer getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(Integer stockDisponible) { this.stockDisponible = stockDisponible; }

    public String getImagenArchivo() { return imagenArchivo; }
    public void setImagenArchivo(String imagenArchivo) { this.imagenArchivo = imagenArchivo; }

    public Double getRatingPromedio() { return ratingPromedio; }
    public void setRatingPromedio(Double ratingPromedio) { this.ratingPromedio = ratingPromedio; }

    public Integer getTotalResenias() { return totalResenias; }
    public void setTotalResenias(Integer totalResenias) { this.totalResenias = totalResenias; }
}
