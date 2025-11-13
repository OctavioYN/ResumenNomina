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
public class ResumenARIMA {
    private Integer totalIndicadoresEvaluados;
    private Integer modelosRobustos;
    private Integer modelosNoRobustos;
    private Integer indicadoresFueraDeRango;
    private Double porcentajeFueraDeRango;
}
