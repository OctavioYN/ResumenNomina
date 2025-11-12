package com.resumen.nomina.alertas.zscore.domain.model;


import com.resumen.nomina.alertas.shared.domain.AlertSeverity;
import lombok.Builder;
import lombok.Data;

/**
 * 🎨 DTO PARA VISUALIZACIÓN DE RESULTADOS Z-SCORE
 * Valores en formato porcentaje para frontend
 */
@Data
@Builder
public class ZScoreResultVisual {
    // Identificación
    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;
    private String periodoEvaluado;

    // Datos para tabla (porcentajes)
    private Double variacionPorcentualVsSA;  // "Variación Porcentual vs S.A."
    private Double variacionMedia;           // "Variación Media"
    private Double desviacionEstandar;       // Desviación estándar
    private Double limiteInferior;           // "Límite Inferior"
    private Double limiteSuperior;           // "Límite Superior"
    private Double zScore;                   // Z-Score calculado
    private Double zScoreAbsoluto;           // Para ordenamiento

    // Severidad
    private AlertSeverity severidad;
    private String colorSeveridad;

    // Estado
    private Boolean alertaActiva;
    private Integer cantidadPeriodosHistoricos;
}