package com.resumen.nomina.alertas.arima.domain.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 📦 RESPUESTA COMPLETA ARIMA
 */
@Data
@Builder
public class ArimaResponse {

    private Boolean success;
    private String message;

    // Contexto
    private String periodo;
    private String sucursal;

    // Resumen
    private Integer totalEvaluados;
    private Integer alertasNormales;
    private Integer alertasActivas;
    private Integer sinDatoActual;
    private Integer modelosInvalidos;

    // Resultados detallados
    private List<ArimaResult> modelosRobustos;

    /**
     * Response exitosa
     */
    public static ArimaResponse exito(String periodo, String sucursal,
                                      List<ArimaResult> alertas,
                                      int sinDato, int invalidos) {

        long normales = alertas.stream()
                .filter(a -> "NORMAL".equals(a.getSeveridad())).count();
        long activas = alertas.stream()
                .filter(a -> "ALERTA".equals(a.getSeveridad())).count();

        return ArimaResponse.builder()
                .success(true)
                .message("Cálculo completado exitosamente")
                .periodo(periodo)
                .sucursal(sucursal)
                .totalEvaluados(alertas.size())
                .alertasNormales((int)normales)
                .alertasActivas((int)activas)
                .sinDatoActual(sinDato)
                .modelosInvalidos(invalidos)
                .modelosRobustos(alertas)
                .build();
    }

    /**
     * Response de error
     */
    public static ArimaResponse error(String mensaje) {
        return ArimaResponse.builder()
                .success(false)
                .message(mensaje)
                .totalEvaluados(0)
                .alertasNormales(0)
                .alertasActivas(0)
                .sinDatoActual(0)
                .modelosInvalidos(0)
                .modelosRobustos(List.of())
                .build();
    }
}