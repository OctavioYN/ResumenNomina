package com.resumen.nomina.alertas.zscore.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 📊 RESULTADO Z-SCORE - FORMATO EXACTO - CORREGIDO
 *
 * Con lógica mejorada para "fueraDeRango"
 */
@Data
@Builder
public class ZScoreResult {

    // Identificación
    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;

    // Datos principales (en porcentaje)
    private Double variacionPorcentualVsSA;
    private Double variacionMedia;
    private Double desviacionEstandar;
    private Double limiteInferior;
    private Double limiteSuperior;

    // Severidad
    private String severidad;           // "NORMAL", "MODERADA", "ALTA", "CRITICA"
    private String colorSeveridad;      // "#4CAF50", "#FFC107", "#FF9800", "#F44336"

    // Estado
    private Boolean fueraDeRango;
    private Integer cantidadPeriodosHistoricos;

    // Z-Score
    @JsonProperty("zscoreAbsoluto")
    private Double zscoreAbsoluto;

    @JsonProperty("zscore")
    private Double zscore;

    /**
     * Redondea un valor a N decimales
     */
    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /**
     * 🔴 CORREGIDO: Constructor con lógica mejorada
     *
     * fueraDeRango ahora considera:
     * 1. Que la variación supere los límites
     * 2. Que la diferencia sea significativa (> 1%)
     * 3. Que el Z-Score sea significativo (|Z| > 1.0)
     */
    public static ZScoreResult crear(
            String puesto, String indicador, Integer conceptoDetalle,
            String sucursal, Integer negocio,
            double variacionActual, double media, double desviacion,
            double limInf, double limSup, int periodos) {

        // Calcular Z-Score
        double z = desviacion == 0 ? 0 : (variacionActual - media) / desviacion;
        double absZ = Math.abs(z);

        // Determinar severidad (basado SOLO en |Z-Score|)
        String sev;
        String color;

        if (absZ >3) {
            sev = "CRITICA";
            color = "#F44336";
        } else if (absZ >= 2) {
            sev = "ALTA";
            color = "#FF9800";
        } else if (absZ >= 1.0) {
            sev = "MODERADA";
            color = "#FFC107";
        } else {
            sev = "NORMAL";
            color = "#4CAF50";
        }


    // AHORA (según PDF):
    /*    if (absZ > 3.0) sev = "CRITICA";      // z_score > 3
        else if (absZ > 2.0) sev = "ALTA";    // z_score > 2
        else if (absZ > 1.0) sev = "MODERADA"; // TRUE (implícito: 1 < z ≤ 2)
        else sev = "NORMAL";                   // z ≤ 1
     */
        // EXACTO del PDF:



        // 🔴 VALIDACIÓN TRIPLE para "fueraDeRango"
        // Según PDF: Debe cumplir las 3 condiciones

        // 1. Supera umbrales dinámicos
        boolean superaUmbrales = variacionActual < limInf || variacionActual > limSup;

        // 2. Diferencia significativa (> 1%)
        boolean diferenciaSignificativa = Math.abs(variacionActual - media) > 0.01;

        // 3. Z-Score significativo (|Z| > 1.0)
        boolean zScoreSignificativo = absZ > 1.0;

        // ✅ FUERA DE RANGO solo si cumple LAS 3 condiciones
        boolean fuera = superaUmbrales && diferenciaSignificativa && zScoreSignificativo;

        return ZScoreResult.builder()
                .puesto(puesto != null ? puesto.trim() : "")
                .indicador(indicador != null ? indicador.trim() : "")
                .conceptoDetalle(conceptoDetalle)
                .sucursal(sucursal != null ? sucursal.trim() : "")
                .negocio(negocio)
                .variacionPorcentualVsSA(round(variacionActual * 100, 2))
                .variacionMedia(round(media * 100, 2))
                .desviacionEstandar(round(desviacion * 100, 2))
                .limiteInferior(round(limInf * 100, 2))
                .limiteSuperior(round(limSup * 100, 2))
                .severidad(sev)
                .colorSeveridad(color)
                .fueraDeRango(fuera)
                .cantidadPeriodosHistoricos(periodos)
                .zscoreAbsoluto(round(absZ, 2))
                .zscore(round(z, 2))
                .build();
    }
}