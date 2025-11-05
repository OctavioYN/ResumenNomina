package com.resumen.nomina.domain.model;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class IndicadorPromedioResponse {
    private Integer negocio;
    private Integer puesto;
    private Integer conceptoDetalle;
    private String semanaActual;
    private String semanaAnterior;
    private Double promedioActual;
    private Double promedioAnterior;
    private Double diferencia;
    private Double variacionPorcentual;
    private LocalDateTime fechaCalculo;

    public IndicadorPromedioResponse() {}

    public IndicadorPromedioResponse(Integer negocio, Integer puesto, Integer conceptoDetalle,
                                     String semanaActual, String semanaAnterior,
                                     Double promedioActual, Double promedioAnterior,
                                     Double diferencia, Double variacionPorcentual) {
        this.negocio = negocio;
        this.puesto = puesto;
        this.conceptoDetalle = conceptoDetalle;
        this.semanaActual = semanaActual;
        this.semanaAnterior = semanaAnterior;
        this.promedioActual = promedioActual;
        this.promedioAnterior = promedioAnterior;
        this.diferencia = diferencia;
        this.variacionPorcentual = variacionPorcentual;
        this.fechaCalculo = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "IndicadorPromedioResponse{" +
                "negocio=" + negocio +
                ", puesto=" + puesto +
                ", conceptoDetalle=" + conceptoDetalle +
                ", semanaActual='" + semanaActual + '\'' +
                ", promedioActual=" + promedioActual +
                ", variacionPorcentual=" + variacionPorcentual +
                '}';
    }
}