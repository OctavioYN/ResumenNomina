package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

// DTO para cada punto de la gráfica
@Setter
@Getter
public class PuntoGrafica {
    // Getters y Setters
    private String semana;
    private Long total;
    private Double variacionVsSA;
    private Long lineaMedia;
    private Long lineaSuper1DS;
    private Long lineaInferior1DS;
    private Long lineaSuper15DS;
    private Long lineaInferior15DS;

    // Constructores
    public PuntoGrafica() {}

    public PuntoGrafica(String semana, Long total, Double variacionVsSA,
                        Long lineaMedia, Long lineaSuper1DS, Long lineaInferior1DS,
                        Long lineaSuper15DS, Long lineaInferior15DS) {
        this.semana = semana;
        this.total = total;
        this.variacionVsSA = variacionVsSA;
        this.lineaMedia = lineaMedia;
        this.lineaSuper1DS = lineaSuper1DS;
        this.lineaInferior1DS = lineaInferior1DS;
        this.lineaSuper15DS = lineaSuper15DS;
        this.lineaInferior15DS = lineaInferior15DS;
    }

    @Override
    public String toString() {
        return "PuntoGrafica{" +
                "semana='" + semana + '\'' +
                ", total=" + total +
                ", variacionVsSA=" + variacionVsSA +
                ", lineaMedia=" + lineaMedia +
                ", lineaSuper1DS=" + lineaSuper1DS +
                ", lineaInferior1DS=" + lineaInferior1DS +
                ", lineaSuper15DS=" + lineaSuper15DS +
                ", lineaInferior15DS=" + lineaInferior15DS +
                '}';
    }
}