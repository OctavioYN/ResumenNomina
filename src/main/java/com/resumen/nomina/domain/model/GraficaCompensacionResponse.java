package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// DTO para respuesta de gráfica histórica
@Setter
@Getter
public class GraficaCompensacionResponse {
    // Getters y Setters
    private EstadisticasCompensacion estadisticas;
    private LineasConfianza lineasConfianza;
    private List<PuntoGrafica> datosGrafica;
    private LocalDateTime fechaConsulta;
    private String periodoAnalisis;

    // Constructores
    public GraficaCompensacionResponse() {
        this.fechaConsulta = LocalDateTime.now();
    }

    public GraficaCompensacionResponse(EstadisticasCompensacion estadisticas,
                                       LineasConfianza lineasConfianza,
                                       List<PuntoGrafica> datosGrafica,
                                       String periodoAnalisis) {
        this();
        this.estadisticas = estadisticas;
        this.lineasConfianza = lineasConfianza;
        this.datosGrafica = datosGrafica;
        this.periodoAnalisis = periodoAnalisis;
    }

    @Override
    public String toString() {
        return "GraficaCompensacionResponse{" +
                "estadisticas=" + estadisticas +
                ", lineasConfianza=" + lineasConfianza +
                ", datosGrafica=" + (datosGrafica != null ? datosGrafica.size() + " puntos" : "null") +
                ", fechaConsulta=" + fechaConsulta +
                ", periodoAnalisis='" + periodoAnalisis + '\'' +
                '}';
    }
}