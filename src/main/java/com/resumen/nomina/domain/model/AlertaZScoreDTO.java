package com.resumen.nomina.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

// ========== DTOs PARA ALERTAS Z-SCORE (Imagen 1) ==========

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaZScoreDTO {
    // Identificación
    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;

    // Datos para tabla (según Imagen 1)
    private Double variacionPorcentualVsSA;  // "Variación Porcentual vs S.A."
    private Double variacionMedia;           // "Variación Media"
    private Double desviacionEstandar;       // Para cálculos internos
    private Double limiteInferior;           // "Límite Inferior"
    private Double limiteSuperior;           // "Límite Superior"
    private Double zScore;                   // Z-Score calculado
    private Double zScoreAbsoluto;           // Para ordenamiento

    // Severidad (colores según imagen)
    private SeveridadAlerta severidad;       // CRÍTICA, ALTA, MODERADA, NORMAL
    private String colorSeveridad;           // #F44336, #FF9800, #4CAF50

    // Metadata
    private boolean fueraDeRango;            // primitive boolean para usar isFueraDeRango()
    private Integer cantidadPeriodosHistoricos;
}