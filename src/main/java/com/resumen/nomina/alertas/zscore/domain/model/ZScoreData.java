package com.resumen.nomina.alertas.zscore.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 📊 DATOS DE ORIGEN PARA CÁLCULO Z-SCORE
 * Mapea directamente desde la colección IndicadoresCalculados
 */
@Data
@Builder
public class ZScoreData {
    // Identificación
    private String puesto;           // fcDetalle5
    private String indicador;        // fcDetalle6
    private Integer conceptoDetalle; // conceptoDetalle
    private String sucursal;         // sucursal
    private Integer negocio;         // negocio
    private String periodo;          // periodoActual

    // Datos para cálculo
    private Double variacion;        // variación porcentual
    private Double valorActual;      // valor actual del indicador

    // Metadata
    private LocalDateTime fechaCalculo;
    private String fuenteDatos;

    // 🔴 VALIDACIONES DE INTEGRIDAD
    public boolean isValid() {
        return puesto != null && !puesto.trim().isEmpty() &&
                indicador != null && !indicador.trim().isEmpty() &&
                variacion != null &&
                periodo != null && !periodo.trim().isEmpty();
    }

    public boolean hasMinimumData() {
        return isValid() && conceptoDetalle != null && sucursal != null && negocio != null;
    }
}