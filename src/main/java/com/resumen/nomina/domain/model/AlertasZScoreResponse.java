package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertasZScoreResponse {
    private String tipo;                    // "Z_SCORE"
    private String titulo;                  // "Z-Score"
    private String descripcion;
    private List<String> consideraciones;
    private String periodoActual;
    private String sucursal;
    private ResumenZScore resumen;
    private List<AlertaZScoreDTO> alertas;
    private LocalDateTime fechaGeneracion;
}
