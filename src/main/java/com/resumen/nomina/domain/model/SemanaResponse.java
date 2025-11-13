

package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanaResponse {
    private Integer anio;
    private Integer numeroSemana;
    private String fechaInicio; // Formato: yyyy-MM-dd
    private String fechaFin;    // Formato: yyyy-MM-dd
    private String descripcion; // Ejemplo: "Semana 39 - 2025"
    private String semana; // Ejemplo: "202539"
}