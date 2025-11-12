package com.resumen.nomina.alertas.arima.domain.model;


import lombok.Builder;
import lombok.Data;

/**
 * 📐 MODELO ARIMA AJUSTADO
 *
 * Representa un modelo ARIMA(p,d,q) ajustado a una serie temporal
 */
@Data
@Builder
public class ArimaModel {

    // ========== PARÁMETROS DEL MODELO ==========

    /**
     * Orden del componente autorregresivo (AR)
     */
    private int p;

    /**
     * Orden de diferenciación (I)
     */
    private int d;

    /**
     * Orden del componente de media móvil (MA)
     */
    private int q;

    // ========== COEFICIENTES ==========

    /**
     * Coeficientes AR (φ₁, φ₂, ..., φₚ)
     */
    private double[] coeficientesAR;

    /**
     * Coeficientes MA (θ₁, θ₂, ..., θ_q)
     */
    private double[] coeficientesMA;

    /**
     * Intercepto (constante)
     */
    private double intercepto;

    // ========== MÉTRICAS DE CALIDAD ==========

    /**
     * AIC (Akaike Information Criterion)
     * Menor es mejor
     */
    private double aic;

    /**
     * BIC (Bayesian Information Criterion)
     * Menor es mejor
     */
    private double bic;

    /**
     * Error estándar de los residuos (σ)
     */
    private double errorEstandar;

    /**
     * R² ajustado
     */
    private double r2Ajustado;

    /**
     * Varianza de los residuos
     */
    private double varianzaResiduos;

    // ========== INFORMACIÓN ADICIONAL ==========

    /**
     * Número de observaciones usadas en el ajuste
     */
    private int numeroObservaciones;

    /**
     * Serie fue diferenciada
     */
    private boolean fueDiferenciada;

    /**
     * Media de la serie original
     */
    private double mediaOriginal;

    /**
     * ¿El modelo es estacionario?
     */
    private boolean esEstacionario;

    /**
     * Residuos del modelo
     */
    private double[] residuos;

    /**
     * Retorna la notación del modelo
     */
    public String getNotacion() {
        return String.format("ARIMA(%d,%d,%d)", p, d, q);
    }

    /**
     * Valida si el modelo es adecuado
     */
    public boolean esValido() {
        return numeroObservaciones >= (p + d + q + 1) * 2
                && !Double.isNaN(errorEstandar)
                && !Double.isInfinite(errorEstandar)
                && errorEstandar > 0;
    }
}