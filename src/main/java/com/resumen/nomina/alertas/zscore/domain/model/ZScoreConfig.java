package com.resumen.nomina.alertas.zscore.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * ⚙️ CONFIGURACIÓN Z-SCORE - SEGÚN PDF ORIGINAL
 *
 * Documento: "Alertas de compensación - Z-Score"
 * Página 2: Estrategia adaptativa de umbrales dinámicos
 * Página 3: Clasificación por severidad
 */
@Data
@Builder
public class ZScoreConfig {

    // Períodos históricos
    private int periodosMinimos;
    private int ventanaAnalisis;

    // ========== ESTRATEGIA ADAPTATIVA (Página 2) ==========
    // σ < 1%: margen fijo de 1.5%
    // 1% ≤ σ < 5%: factor de 1.3×σ
    // σ ≥ 5%: usa σ directamente

    private double umbralEstable;          // 0.01 (1%)
    private double umbralVolatilidad;      // 0.05 (5%)
    private double factorEstable;          // 1.5
    private double factorMedio;            // 1.3

    // Límites de margen: [1%, 20%]
    private double margenMinimo;           // 0.01 (1%)
    private double margenMaximo;           // 0.20 (20%)

    // ========== VALIDACIÓN TRIPLE (Página 3) ==========
    // 1. Superar umbrales dinámicos
    // 2. Diferencia absoluta > 1%
    // 3. Z-score > 1

    private double umbralDiferencia;       // 0.01 (1%)
    private double umbralZScore;           // 1.0

    // ========== CLASIFICACIÓN POR SEVERIDAD (Página 3) ==========
    // z_score > 3 ~ "CRITICA"
    // z_score > 2 ~ "ALTA"
    // TRUE (1 < z ≤ 2) ~ "MODERADA"

    private double umbralCritico;          // 3.0
    private double umbralAlto;             // 2.0
    private double umbralModerado;         // 1.0

    // Exclusiones
    private Integer conceptoExcluir;

    /**
     * 🔴 CONFIGURACIÓN SEGÚN PDF ORIGINAL
     * Esta es la configuración EXACTA del documento
     */
    public static ZScoreConfig porDefecto() {
        return ZScoreConfig.builder()
                .periodosMinimos(12)
                .ventanaAnalisis(52)

                // Estrategia adaptativa (Página 2)
                .umbralEstable(0.01)           // 1%
                .umbralVolatilidad(0.05)       // 5%
                .factorEstable(1.5)            // 1.5×
                .factorMedio(1.3)              // 1.3×

                // Límites de seguridad
                .margenMinimo(0.01)            // 1%
                .margenMaximo(0.20)            // 20%

                // Validación triple (Página 3)
                .umbralDiferencia(0.01)        // > 1%
                .umbralZScore(1.0)             // > 1σ

                // Severidad (Página 3)
                .umbralCritico(3.0)            // > 3σ
                .umbralAlto(2.0)               // > 2σ
                .umbralModerado(1.0)           // > 1σ

                .conceptoExcluir(1011)
                .build();
    }

    /**
     * Configuración más conservadora (menos alertas)
     * Útil si hay demasiadas alertas con la configuración por defecto
     */
    public static ZScoreConfig conservadora() {
        return ZScoreConfig.builder()
                .periodosMinimos(12)
                .ventanaAnalisis(52)
                .umbralEstable(0.01)
                .umbralVolatilidad(0.05)
                .factorEstable(2.0)            // Más permisivo
                .factorMedio(1.5)              // Más permisivo
                .margenMinimo(0.02)            // 2%
                .margenMaximo(0.30)            // 30%
                .umbralDiferencia(0.02)        // 2%
                .umbralZScore(1.5)             // 1.5σ
                .umbralCritico(3.5)            // 3.5σ
                .umbralAlto(2.5)               // 2.5σ
                .umbralModerado(1.5)           // 1.5σ
                .conceptoExcluir(1011)
                .build();
    }

    /**
     * Configuración más estricta (más alertas)
     * Útil para detección temprana de anomalías
     */
    public static ZScoreConfig estricta() {
        return ZScoreConfig.builder()
                .periodosMinimos(12)
                .ventanaAnalisis(52)
                .umbralEstable(0.01)
                .umbralVolatilidad(0.05)
                .factorEstable(1.2)            // Menos permisivo
                .factorMedio(1.1)              // Menos permisivo
                .margenMinimo(0.005)           // 0.5%
                .margenMaximo(0.15)            // 15%
                .umbralDiferencia(0.005)       // 0.5%
                .umbralZScore(0.5)             // 0.5σ
                .umbralCritico(2.5)            // 2.5σ
                .umbralAlto(1.5)               // 1.5σ
                .umbralModerado(0.8)           // 0.8σ
                .conceptoExcluir(1011)
                .build();
    }
}