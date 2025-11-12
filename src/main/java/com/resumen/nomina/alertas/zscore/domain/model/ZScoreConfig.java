package com.resumen.nomina.alertas.zscore.domain.model;


import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * ⚙️ CONFIGURACIÓN ESPECÍFICA PARA Z-SCORE
 * Basada en los requerimientos del PDF
 */
@Data
@Builder
public class ZScoreConfig {
    // ========== IDENTIFICACIÓN ==========
    private String codigoConfiguracion;
    private String descripcion;
    private boolean activa;

    // ========== PERÍODOS HISTÓRICOS ==========
    private int periodosMinimosHistoricos;  // Mínimo para cálculo (default: 12)
    private int ventanaAnalisis;           // Períodos a considerar (default: 52)

    // ========== ESTRATEGIA ADAPTATIVA ==========
    private double umbralEstable;          // 1% para series estables
    private double umbralVolatilidad;      // 5% para volatilidad media
    private double factorSeriesEstables;   // 1.5 para σ < 1%
    private double factorVolatilidadMedia; // 1.3 para 1% ≤ σ < 5%
    private double limiteMinimoMargen;     // 1% mínimo
    private double limiteMaximoMargen;     // 20% máximo

    // ========== VALIDACIÓN TRIPLE (PDF) ==========
    private double umbralDiferenciaMinima; // 1% diferencia mínima
    private double umbralZScoreMinimo;     // 1.0 Z-Score mínimo
    private boolean usarValidacionTriple;  // Habilitar 3 condiciones

    // ========== UMBRALES SEVERIDAD ==========
    private double umbralCritico;          // 2.5
    private double umbralAlto;             // 1.96
    private double umbralModerado;         // 1.0

    // ========== EXCLUSIONES ==========
    private List<Integer> conceptosExcluidos;
    private List<Integer> negociosExcluidos;
    private List<String> puestosExcluidos;

    // ========== CONFIGURACIÓN POR DEFECTO ==========
    public static ZScoreConfig crearConfiguracionPorDefecto() {
        return ZScoreConfig.builder()
                .codigoConfiguracion("ZSCORE_DEFAULT")
                .descripcion("Configuración por defecto Z-Score según PDF")
                .activa(true)
                .periodosMinimosHistoricos(12)
                .ventanaAnalisis(52)
                .umbralEstable(0.01)           // 1%
                .umbralVolatilidad(0.05)       // 5%
                .factorSeriesEstables(1.5)     // 1.5×
                .factorVolatilidadMedia(1.3)   // 1.3×
                .limiteMinimoMargen(0.01)      // 1%
                .limiteMaximoMargen(0.20)      // 20%
                .umbralDiferenciaMinima(0.01)  // 1%
                .umbralZScoreMinimo(1.0)       // Z-Score mínimo 1.0
                .usarValidacionTriple(true)    // Habilitar validación triple
                .umbralCritico(2.5)
                .umbralAlto(1.96)
                .umbralModerado(1.0)
                .conceptosExcluidos(List.of(1011)) // Empleados
                .negociosExcluidos(List.of())
                .puestosExcluidos(List.of())
                .build();
    }

    // 🔴 VALIDACIÓN DE CONFIGURACIÓN
    public void validar() {
        if (periodosMinimosHistoricos < 1) {
            throw new IllegalArgumentException("periodosMinimosHistoricos debe ser >= 1");
        }
        if (umbralCritico <= umbralAlto) {
            throw new IllegalArgumentException("umbralCritico debe ser > umbralAlto");
        }
        if (umbralAlto <= umbralModerado) {
            throw new IllegalArgumentException("umbralAlto debe ser > umbralModerado");
        }
        if (limiteMinimoMargen >= limiteMaximoMargen) {
            throw new IllegalArgumentException("limiteMinimoMargen debe ser < limiteMaximoMargen");
        }
    }
}