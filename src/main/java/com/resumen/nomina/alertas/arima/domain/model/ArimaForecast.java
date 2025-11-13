package com.resumen.nomina.alertas.arima.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 🔮 PRONÓSTICO ARIMA
 *
 * Según PDF Página 3: Intervalo de predicción = ŷ ± 1.96 × SE
 */
@Data
@Builder
public class ArimaForecast {

    /**
     * Valor pronosticado (ŷ)
     */
    private double pronostico;

    /**
     * Límite inferior del intervalo de predicción al 95%
     * LI = ŷ - 1.96 × SE
     */
    private double limiteInferior;

    /**
     * Límite superior del intervalo de predicción al 95%
     * LS = ŷ + 1.96 × SE
     */
    private double limiteSuperior;

    /**
     * Error estándar del pronóstico (SE)
     */
    private double errorEstandar;

    /**
     * Nivel de confianza (ej: 0.95 para 95%)
     */
    private double nivelConfianza;

    /**
     * Ancho del intervalo
     */
    public double getAnchoIntervalo() {
        return limiteSuperior - limiteInferior;
    }

    /**
     * Porcentaje de incertidumbre respecto al pronóstico
     */
    public double getPorcentajeIncertidumbre() {
        if (pronostico == 0) return 0;
        return (getAnchoIntervalo() / Math.abs(pronostico)) * 100;
    }
}