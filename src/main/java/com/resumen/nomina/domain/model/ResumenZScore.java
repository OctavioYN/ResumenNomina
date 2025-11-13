package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenZScore {
    private Integer totalIndicadoresEvaluados;
    private Integer alertasCriticas;
    private Integer alertasAltas;
    private Integer alertasModeradas;
    private Integer alertasNormales;
    private Integer indicadoresFueraDeRango;
    private Double porcentajeFueraDeRango;
}
