package com.resumen.nomina.alertas.zscore.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 📨 RESPUESTA DEL CÁLCULO Z-SCORE
 */
@Data
@Builder
public class ZScoreCalculationResponse {
    private boolean success;
    private String message;
    private String error;

    // Contexto
    private String periodoActual;
    private String sucursal;
    private String configuracionUsada;
    private LocalDateTime fechaCalculo;

    // Resultados
    private ZScoreSummary resumen;
    private List<ZScoreResultVisual> resultados;
    private Integer totalEvaluados;

    // Métodos de utilidad
    public static ZScoreCalculationResponse crearError(String mensajeError) {
        return ZScoreCalculationResponse.builder()
                .success(false)
                .error(mensajeError)
                .fechaCalculo(LocalDateTime.now())
                .build();
    }

    public static ZScoreCalculationResponse crearExito(String periodoActual, String sucursal,
                                                       String configuracion, ZScoreSummary resumen,
                                                       List<ZScoreResultVisual> resultados) {
        return ZScoreCalculationResponse.builder()
                .success(true)
                .message("Cálculo Z-Score completado exitosamente")
                .periodoActual(periodoActual)
                .sucursal(sucursal)
                .configuracionUsada(configuracion)
                .resumen(resumen)
                .resultados(resultados)
                .totalEvaluados(resultados.size())
                .fechaCalculo(LocalDateTime.now())
                .build();
    }
}