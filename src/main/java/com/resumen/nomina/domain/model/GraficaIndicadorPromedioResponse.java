package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class GraficaIndicadorPromedioResponse {
    private Integer negocio;
    private Integer puesto;
    private Integer conceptoDetalle;
    private EstadisticasCompensacion estadisticas;
    private LineasConfianza lineasConfianza;
    private List<PuntoGraficaPromedio> datosGrafica;
    private LocalDateTime fechaConsulta;
    private String periodoAnalisis;

    public GraficaIndicadorPromedioResponse() {
        this.fechaConsulta = LocalDateTime.now();
    }

    public GraficaIndicadorPromedioResponse(Integer negocio, Integer puesto, Integer conceptoDetalle,
                                            EstadisticasCompensacion estadisticas,
                                            LineasConfianza lineasConfianza,
                                            List<PuntoGraficaPromedio> datosGrafica,
                                            String periodoAnalisis) {
        this();
        this.negocio = negocio;
        this.puesto = puesto;
        this.conceptoDetalle = conceptoDetalle;
        this.estadisticas = estadisticas;
        this.lineasConfianza = lineasConfianza;
        this.datosGrafica = datosGrafica;
        this.periodoAnalisis = periodoAnalisis;
    }

    @Override
    public String toString() {
        return "GraficaIndicadorPromedioResponse{" +
                "negocio=" + negocio +
                ", puesto=" + puesto +
                ", conceptoDetalle=" + conceptoDetalle +
                ", datosGrafica=" + (datosGrafica != null ? datosGrafica.size() + " puntos" : "null") +
                ", periodoAnalisis='" + periodoAnalisis + '\'' +
                '}';
    }
}
