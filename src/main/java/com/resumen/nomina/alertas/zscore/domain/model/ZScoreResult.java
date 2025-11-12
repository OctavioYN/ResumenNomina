package com.resumen.nomina.alertas.zscore.domain.model;


import com.resumen.nomina.alertas.shared.domain.AlertSeverity;
import com.resumen.nomina.alertas.zscore.application.util.ZScoreNumberHelper;
import lombok.Builder;
import lombok.Data;

/**
 * 📊 RESULTADO INDIVIDUAL DE CÁLCULO Z-SCORE
 */
@Data
@Builder
public class ZScoreResult {
    // Identificación
    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;
    private String periodoEvaluado;

    // Datos de cálculo (en formato decimal para procesamiento)
    private double variacionActual;
    private double variacionMedia;
    private double desviacionEstandar;
    private double zScore;

    // Límites adaptativos (decimal)
    private double limiteInferior;
    private double limiteSuperior;
    private double margenAdaptativo;

    // Resultados de validación
    private boolean superaUmbrales;
    private boolean diferenciaSignificativa;
    private boolean zScoreSignificativo;
    private boolean alertaActiva;

    // Severidad
    private AlertSeverity severidad;
    private String colorSeveridad;
    private String emojiSeveridad;

    // Metadata
    private int cantidadPeriodosHistoricos;
    private boolean datosSuficientes;
    private String mensajeAdvertencia;

    /**
     * Convierte valores a porcentaje para visualización
     */
    public ZScoreResultVisual toVisual() {
        return ZScoreResultVisual.builder()
                .puesto(puesto)
                .indicador(indicador)
                .conceptoDetalle(conceptoDetalle)
                .sucursal(sucursal)
                .negocio(negocio)
                .periodoEvaluado(periodoEvaluado)
                .variacionPorcentualVsSA(ZScoreNumberHelper.redondear(variacionActual * 100, 2))
                .variacionMedia(ZScoreNumberHelper.redondear(variacionMedia * 100, 2))
                .desviacionEstandar(ZScoreNumberHelper.redondear(desviacionEstandar * 100, 2))
                .limiteInferior(ZScoreNumberHelper.redondear(limiteInferior * 100, 2))
                .limiteSuperior(ZScoreNumberHelper.redondear(limiteSuperior * 100, 2))
                .zScore(ZScoreNumberHelper.redondear(zScore, 2))
                .zScoreAbsoluto(Math.abs(ZScoreNumberHelper.redondear(zScore, 2)))
                .severidad(severidad)
                .colorSeveridad(severidad.getColorHex())
                .alertaActiva(alertaActiva)
                .cantidadPeriodosHistoricos(cantidadPeriodosHistoricos)
                .build();
    }
}