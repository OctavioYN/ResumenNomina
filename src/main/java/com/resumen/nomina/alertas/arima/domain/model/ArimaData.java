package com.resumen.nomina.alertas.arima.domain.model;


import lombok.Builder;
import lombok.Data;

/**
 * 📊 DATOS DE ENTRADA ARIMA
 *
 * Representa un punto en la serie temporal
 */
@Data
@Builder
public class ArimaData {

    // Identificación
    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;

    // Serie temporal
    private String periodo;           // Período (ej: "202544")
    private Double valor;             // Valor del indicador (ya normalizado)

    /**
     * Genera clave única para agrupar series temporales
     */
    public String getClave() {
        return String.format("%s|%s|%d|%s|%d",
                puesto != null ? puesto.trim() : "",
                indicador != null ? indicador.trim() : "",
                conceptoDetalle != null ? conceptoDetalle : 0,
                sucursal != null ? sucursal.trim() : "",
                negocio != null ? negocio : 0);
    }
}