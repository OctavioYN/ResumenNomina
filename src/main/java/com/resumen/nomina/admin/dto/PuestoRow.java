package com.resumen.nomina.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuestoRow {
    private Integer idPuesto;
    private Integer idFuncion;
    private String puesto;
    private List<IndicadorRow> indicadores;
}
