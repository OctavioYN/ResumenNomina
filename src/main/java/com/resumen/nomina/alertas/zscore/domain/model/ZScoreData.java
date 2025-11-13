package com.resumen.nomina.alertas.zscore.domain.model;


import lombok.Builder;
import lombok.Data;

/**
 * 📊 DATOS DE ENTRADA PARA Z-SCORE
 * Mapea desde IndicadoresCalculados
 */
@Data
@Builder
public class ZScoreData {
    private String puesto;              // fcDetalle5
    private String indicador;           // fcDetalle6
    private Integer conceptoDetalle;    // conceptoDetalle
    private String sucursal;            // sucursal
    private Integer negocio;            // negocio
    private String periodo;             // periodoActual
    private Double variacion;           // variación (decimal: 0.2190 = 21.90%)
}