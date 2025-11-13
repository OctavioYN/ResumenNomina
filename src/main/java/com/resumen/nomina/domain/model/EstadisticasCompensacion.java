package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

// DTO para estadísticas de líneas de confianza
@Setter
@Getter
public class EstadisticasCompensacion {
    // Getters y Setters
    private Long media;
    private Long desviacionEstandar;
    private Integer totalSemanas;
    private String periodoAnalisis;

    // Constructores
    public EstadisticasCompensacion() {}

    public EstadisticasCompensacion(Long media, Long desviacionEstandar,
                                    Integer totalSemanas, String periodoAnalisis) {
        this.media = media;
        this.desviacionEstandar = desviacionEstandar;
        this.totalSemanas = totalSemanas;
        this.periodoAnalisis = periodoAnalisis;
    }

    @Override
    public String toString() {
        return "EstadisticasCompensacion{" +
                "media=" + media +
                ", desviacionEstandar=" + desviacionEstandar +
                ", totalSemanas=" + totalSemanas +
                ", periodoAnalisis='" + periodoAnalisis + '\'' +
                '}';
    }
}