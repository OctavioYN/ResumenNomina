package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaARIMADTO {
    // Identificación
    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;

    // Datos para tabla (según Imagen 2)
    private Double observacionReal;          // "Observación Real"
    private Double limiteInferior;           // "Límite Inferior"
    private Double limiteSuperior;           // "Límite Superior"
    private Double rangoPrediccion;          // Rango = Superior - Inferior
    private Double variacionFueraDelRango;   // "Variación fuera del rango" (%)
    private String direccionDesviacion;      // "SUPERIOR" o "INFERIOR"

    // Estado del modelo
    private boolean fueraDeRango;            // primitive boolean para isFueraDeRango()
    private Integer cantidadPeriodosHistoricos;
    private boolean modeloRobusto;           // primitive boolean para isModeloRobusto()
    private String advertencia;              // Si no es robusto
}
