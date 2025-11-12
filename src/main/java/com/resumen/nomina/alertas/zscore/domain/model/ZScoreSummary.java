package com.resumen.nomina.alertas.zscore.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 📈 RESUMEN ESTADÍSTICO DE RESULTADOS Z-SCORE
 */
@Data
@Builder
public class ZScoreSummary {
    private Integer totalEvaluados;
    private Integer alertasActivas;
    private Integer alertasCriticas;
    private Integer alertasAltas;
    private Integer alertasModeradas;
    private Integer alertasNormales;
    private Double porcentajeAlertas;

    public Double getPorcentajeCriticas() {
        return totalEvaluados == 0 ? 0.0 : (alertasCriticas * 100.0) / totalEvaluados;
    }

    public Double getPorcentajeAltas() {
        return totalEvaluados == 0 ? 0.0 : (alertasAltas * 100.0) / totalEvaluados;
    }
}