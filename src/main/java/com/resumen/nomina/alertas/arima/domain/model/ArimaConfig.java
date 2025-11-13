package com.resumen.nomina.alertas.arima.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * ⚙️ CONFIGURACIÓN ARIMA
 *
 * Según PDF: "Alertas de compensación - Modelo ARIMA"
 * - Ajuste automático del mejor modelo
 * - Intervalo de predicción del 95%
 * - Detección de outliers
 */
@Data
@Builder
public class ArimaConfig {

    // ========== PARÁMETROS DEL MODELO ==========

    /**
     * Períodos mínimos requeridos para ajustar modelo
     * Recomendado: al menos 12 para capturar estacionalidad básica
     */
    private int periodosMinimos;

    /**
     * Ventana de análisis (número máximo de períodos históricos)
     */
    private int ventanaAnalisis;

    /**
     * Nivel de confianza para intervalo de predicción
     * Según PDF: 95% (z = 1.96)
     */
    private double nivelConfianza;

    /**
     * Valor Z para el nivel de confianza
     * 95% → 1.96
     * 99% → 2.576
     */
    private double valorZ;

    // ========== LÍMITES DE BÚSQUEDA ARIMA(p,d,q) ==========

    /**
     * Máximo orden autorregresivo (p)
     * Valor típico: 3-5
     */
    private int maxP;

    /**
     * Máximo orden de diferenciación (d)
     * Valor típico: 1-2
     */
    private int maxD;

    /**
     * Máximo orden de media móvil (q)
     * Valor típico: 3-5
     */
    private int maxQ;

    // ========== CRITERIOS DE SELECCIÓN ==========

    /**
     * Criterio para selección del mejor modelo
     * "AIC" (Akaike) o "BIC" (Bayesian)
     */
    private String criterioSeleccion;

    /**
     * Umbral de significancia para pruebas estadísticas
     */
    private double nivelSignificancia;

    // ========== VALIDACIÓN ==========

    /**
     * Porcentaje mínimo de datos válidos requeridos
     */
    private double porcentajeDatosMinimo;

    /**
     * Excluir conceptos específicos (ej: 1011 = Empleados)
     */
    private Integer conceptoExcluir;

    /**
     * 🔴 CONFIGURACIÓN POR DEFECTO
     * Basada en el documento PDF y mejores prácticas
     */
    public static ArimaConfig porDefecto() {
        return ArimaConfig.builder()
                // Requisitos de datos
                .periodosMinimos(12)        // Al menos 1 año de datos
                .ventanaAnalisis(52)        // Hasta 52 semanas
                .porcentajeDatosMinimo(0.80) // 80% de datos válidos

                // Intervalo de predicción (Página 3 del PDF)
                .nivelConfianza(0.95)       // 95%
                .valorZ(1.96)               // Z-score para 95%

                // Búsqueda de parámetros ARIMA
                .maxP(3)                    // Autorregresivo hasta orden 3
                .maxD(2)                    // Diferenciación hasta orden 2
                .maxQ(3)                    // Media móvil hasta orden 3

                // Selección de modelo
                .criterioSeleccion("AIC")   // Akaike Information Criterion
                .nivelSignificancia(0.05)   // 5% para pruebas

                // Exclusiones
                .conceptoExcluir(1011)
                .build();
    }

    /**
     * Configuración conservadora (menos parámetros, más estable)
     */
    public static ArimaConfig conservadora() {
        return ArimaConfig.builder()
                .periodosMinimos(16)
                .ventanaAnalisis(52)
                .porcentajeDatosMinimo(0.90)
                .nivelConfianza(0.95)
                .valorZ(1.96)
                .maxP(2)                    // Menos parámetros
                .maxD(1)
                .maxQ(2)
                .criterioSeleccion("BIC")   // BIC penaliza más la complejidad
                .nivelSignificancia(0.05)
                .conceptoExcluir(1011)
                .build();
    }

    /**
     * Configuración exhaustiva (busca mejor ajuste, más lento)
     */
    public static ArimaConfig exhaustiva() {
        return ArimaConfig.builder()
                .periodosMinimos(12)
                .ventanaAnalisis(104)       // 2 años
                .porcentajeDatosMinimo(0.70)
                .nivelConfianza(0.95)
                .valorZ(1.96)
                .maxP(5)                    // Búsqueda más amplia
                .maxD(2)
                .maxQ(5)
                .criterioSeleccion("AIC")
                .nivelSignificancia(0.05)
                .conceptoExcluir(1011)
                .build();
    }
}