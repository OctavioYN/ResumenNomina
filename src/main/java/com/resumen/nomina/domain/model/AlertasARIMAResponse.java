package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertasARIMAResponse {
    private String tipo;                     // "ARIMA"
    private String titulo;                   // "Intervalo de Predicción 95%"
    private String descripcion;
    private String advertencia;              // "*Puestos sin suficiente historia..."
    private String periodoActual;
    private String sucursal;
    private ResumenARIMA resumen;
    private List<AlertaARIMADTO> alertas;
    private List<AlertaARIMADTO> modelosRobustos;
    private List<AlertaARIMADTO> modelosNoRobustos;
    private LocalDateTime fechaGeneracion;
}