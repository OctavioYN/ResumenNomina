package com.resumen.nomina.alertas.zscore.domain.model;


import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 📨 RESPUESTA COMPLETA DEL SISTEMA Z-SCORE
 */
@Data
@Builder
public class ZScoreResponse {
    private Boolean success;
    private String message;
    private String periodo;
    private String sucursal;

    // Resumen
    private Integer totalEvaluados;
    private Integer alertasNormales;
    private Integer alertasModeradas;
    private Integer alertasAltas;
    private Integer alertasCriticas;
    private Integer fueraDeRango;

    // Resultados
    private List<ZScoreResult> alertas;

    public static ZScoreResponse error(String mensaje) {
        return ZScoreResponse.builder()
                .success(false)
                .message(mensaje)
                .build();
    }
}