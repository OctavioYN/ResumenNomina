package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenGeneral {
    private Integer totalIndicadores;
    private Integer alertasCriticasZScore;
    private Integer alertasFueraRangoARIMA;
    private Double porcentajeAlertasActivas;
}
