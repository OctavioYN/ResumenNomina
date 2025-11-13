package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertasCompletasResponse {
    private String periodoActual;
    private String sucursal;
    private AlertasZScoreResponse zscoreResponse;
    private AlertasARIMAResponse arimaResponse;
    private ResumenGeneral resumenGeneral;
    private LocalDateTime fechaGeneracion;
}