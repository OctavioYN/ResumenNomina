package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Setter
@Getter
@Document(collection = "IndicadoresCalculados")
public class IndicadorCalculado {
    // Getters y Setters
    @Id
    private String _id;

    // Datos del indicador
    private Integer puesto;
    private String sucursal;
    private String fcDetalle5;
    private String fcDetalle6;
    private Integer negocio;
    private Integer conceptoDetalle;

    // Periodos y valores
    private String periodoAnterior;
    private Double valorAnterior;
    private String periodoActual;
    private Double valorActual;

    // Cálculos
    private Double diferencia;
    private Double variacion;

    // Metadata del cálculo
    private LocalDateTime fechaCalculo;
    private String usuarioCalculo;
    private String versionCalculo;
    private String tipoCalculo; // "AUTOMATICO", "MANUAL", etc.

    // Constructores
    public IndicadorCalculado() {
        this.fechaCalculo = LocalDateTime.now();
        this.tipoCalculo = "AUTOMATICO";
        this.versionCalculo = "1.0";
    }

    public IndicadorCalculado(Integer puesto, String sucursal, String fcDetalle5, String fcDetalle6,
                              Integer negocio, Integer conceptoDetalle, String periodoAnterior,
                              Double valorAnterior, String periodoActual, Double valorActual,
                              Double diferencia, Double variacion) {
        this();
        this.puesto = puesto;
        this.sucursal = sucursal;
        this.fcDetalle5 = fcDetalle5;
        this.fcDetalle6 = fcDetalle6;
        this.negocio = negocio;
        this.conceptoDetalle = conceptoDetalle;
        this.periodoAnterior = periodoAnterior;
        this.valorAnterior = valorAnterior;
        this.periodoActual = periodoActual;
        this.valorActual = valorActual;
        this.diferencia = diferencia;
        this.variacion = variacion;
    }

    @Override
    public String toString() {
        return "IndicadorCalculado{" +
                "puesto=" + puesto +
                ", sucursal='" + sucursal + '\'' +
                ", negocio=" + negocio +
                ", conceptoDetalle=" + conceptoDetalle +
                ", periodoAnterior='" + periodoAnterior + '\'' +
                ", periodoActual='" + periodoActual + '\'' +
                ", diferencia=" + diferencia +
                ", variacion=" + variacion +
                ", fechaCalculo=" + fechaCalculo +
                '}';
    }
}