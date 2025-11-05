package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Setter
@Getter
@Document(collection = "CompensacionesSemana")
public class CompensacionSemanal {
    // Getters y Setters
    @Id
    private String _id;

    // Identificadores únicos
    @Indexed(unique = true)
    private String semana; // "2025-34"

    private Integer anio;
    private Integer numeroSemana;

    // Datos de compensación nacional (todos los negocios)
    private Double totalCompensacionNacional;
    private Integer totalRegistros;
    private Integer cantidadNegocios;
    private Integer cantidadPuestos;

    // Comparación con semana anterior
    private String semanaAnterior;
    private Double totalSemanaAnterior;
    private Double diferenciaPesos;
    private Double variacionPorcentual;

    // Estadísticas históricas (para líneas de confianza)
    private Double mediaHistorica;
    private Double desviacionEstandar;
    private Double lineaSuperior1DS;
    private Double lineaInferior1DS;
    private Double lineaSuperior15DS;
    private Double lineaInferior15DS;

    // Metadata del cálculo
    private LocalDateTime fechaCalculo;
    private String usuarioCalculo;
    private String tipoCalculo;
    private String versionCalculo;
    private Boolean esUltimaSemana; // Flag para identificar la semana más reciente

    // Constructores
    public CompensacionSemanal() {
        this.fechaCalculo = LocalDateTime.now();
        this.tipoCalculo = "AUTOMATICO";
        this.versionCalculo = "1.0";
        this.esUltimaSemana = false;
    }

    public CompensacionSemanal(String semana, Integer anio, Integer numeroSemana,
                               Double totalCompensacionNacional, Integer totalRegistros,
                               Integer cantidadNegocios, Integer cantidadPuestos, String usuario) {
        this();
        this.semana = semana;
        this.anio = anio;
        this.numeroSemana = numeroSemana;
        this.totalCompensacionNacional = totalCompensacionNacional;
        this.totalRegistros = totalRegistros;
        this.cantidadNegocios = cantidadNegocios;
        this.cantidadPuestos = cantidadPuestos;
        this.usuarioCalculo = usuario;
    }

    @Override
    public String toString() {
        return "CompensacionSemanal{" +
                "semana='" + semana + '\'' +
                ", totalCompensacionNacional=" + totalCompensacionNacional +
                ", diferenciaPesos=" + diferenciaPesos +
                ", variacionPorcentual=" + variacionPorcentual +
                ", fechaCalculo=" + fechaCalculo +
                '}';
    }
}