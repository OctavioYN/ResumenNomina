package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import java.time.LocalDateTime;

@Setter
@Getter
@Document(collection = "IndicadoresPromedio")
@CompoundIndexes({
        @CompoundIndex(name = "indicador_key", def = "{'negocio': 1, 'puesto': 1, 'conceptoDetalle': 1, 'semana': 1}", unique = true)
})
public class IndicadorPromedio {
    // Getters y Setters
    @Id
    private String _id;

    private Integer negocio;
    private Integer puesto;
    private Integer conceptoDetalle;

    private String semana;
    private Integer anio;
    private Integer numeroSemana;

    private Double totalIndicador;
    private Integer totalEmpleados;
    private Double promedio;
    private Integer totalRegistros;

    private String semanaAnterior;
    private Double promedioSemanaAnterior;
    private Double diferencia;
    private Double variacionPorcentual;

    private Double mediaHistorica;
    private Double desviacionEstandar;
    private Double lineaSuperior1DS;
    private Double lineaInferior1DS;
    private Double lineaSuperior15DS;
    private Double lineaInferior15DS;

    private LocalDateTime fechaCalculo;
    private String usuarioCalculo;
    private String tipoCalculo;
    private String versionCalculo;
    private Boolean esUltimaSemana;

    public IndicadorPromedio() {
        this.fechaCalculo = LocalDateTime.now();
        this.tipoCalculo = "AUTOMATICO";
        this.versionCalculo = "1.0";
        this.esUltimaSemana = false;
    }

    public IndicadorPromedio(Integer negocio, Integer puesto, Integer conceptoDetalle,
                             String semana, Integer anio, Integer numeroSemana,
                             Double totalIndicador, Integer totalEmpleados,
                             Double promedio, Integer totalRegistros, String usuario) {
        this();
        this.negocio = negocio;
        this.puesto = puesto;
        this.conceptoDetalle = conceptoDetalle;
        this.semana = semana;
        this.anio = anio;
        this.numeroSemana = numeroSemana;
        this.totalIndicador = totalIndicador;
        this.totalEmpleados = totalEmpleados;
        this.promedio = promedio;
        this.totalRegistros = totalRegistros;
        this.usuarioCalculo = usuario;
    }

    @Override
    public String toString() {
        return "IndicadorPromedio{" +
                "negocio=" + negocio +
                ", puesto=" + puesto +
                ", conceptoDetalle=" + conceptoDetalle +
                ", semana='" + semana + '\'' +
                ", promedio=" + promedio +
                '}';
    }
}