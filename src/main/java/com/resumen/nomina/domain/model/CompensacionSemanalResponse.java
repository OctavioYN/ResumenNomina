package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO para respuesta de tabla (comparación semanal actual vs anterior)
@Setter
@Getter
public class CompensacionSemanalResponse {
    // Getters y Setters
    private String semanaActual;
    private String semanaAnterior;
    private Long totalSemanaActual;
    private Long totalSemanaAnterior;
    private Long diferenciaPesos;
    private Double variacionPorcentual;
    private LocalDateTime fechaCalculo;

    // Constructores
    public CompensacionSemanalResponse() {}

    public CompensacionSemanalResponse(String semanaActual, String semanaAnterior,
                                       Long totalSemanaActual, Long totalSemanaAnterior,
                                       Long diferenciaPesos, Double variacionPorcentual) {
        this.semanaActual = semanaActual;
        this.semanaAnterior = semanaAnterior;
        this.totalSemanaActual = totalSemanaActual;
        this.totalSemanaAnterior = totalSemanaAnterior;
        this.diferenciaPesos = diferenciaPesos;
        this.variacionPorcentual = variacionPorcentual;
        this.fechaCalculo = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "CompensacionSemanalResponse{" +
                "semanaActual='" + semanaActual + '\'' +
                ", semanaAnterior='" + semanaAnterior + '\'' +
                ", totalSemanaActual=" + totalSemanaActual +
                ", totalSemanaAnterior=" + totalSemanaAnterior +
                ", diferenciaPesos=" + diferenciaPesos +
                ", variacionPorcentual=" + variacionPorcentual +
                ", fechaCalculo=" + fechaCalculo +
                '}';
    }
}