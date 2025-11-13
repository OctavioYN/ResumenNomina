package com.resumen.nomina.domain.model;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PuntoGraficaPromedio {
    private String semana;
    private Double promedio;
    private Double variacionVsSA;
    private Double lineaMedia;
    private Double lineaSuper1DS;
    private Double lineaInferior1DS;
    private Double lineaSuper15DS;
    private Double lineaInferior15DS;

    public PuntoGraficaPromedio() {}

    public PuntoGraficaPromedio(String semana, Double promedio, Double variacionVsSA,
                                Double lineaMedia, Double lineaSuper1DS, Double lineaInferior1DS,
                                Double lineaSuper15DS, Double lineaInferior15DS) {
        this.semana = semana;
        this.promedio = promedio;
        this.variacionVsSA = variacionVsSA;
        this.lineaMedia = lineaMedia;
        this.lineaSuper1DS = lineaSuper1DS;
        this.lineaInferior1DS = lineaInferior1DS;
        this.lineaSuper15DS = lineaSuper15DS;
        this.lineaInferior15DS = lineaInferior15DS;
    }

    @Override
    public String toString() {
        return "PuntoGraficaPromedio{" +
                "semana='" + semana + '\'' +
                ", promedio=" + promedio +
                ", variacionVsSA=" + variacionVsSA +
                '}';
    }
}