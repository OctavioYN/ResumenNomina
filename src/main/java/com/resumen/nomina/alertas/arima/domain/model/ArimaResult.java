package com.resumen.nomina.alertas.arima.domain.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 📊 RESULTADO DE ALERTA ARIMA
 *
 * Según PDF: "Se activa una alerta cuando el valor observado está fuera
 * del intervalo de predicción del 95% construido con el modelo ARIMA"
 */
@Data
@Builder
public class ArimaResult {

    // ========== IDENTIFICACIÓN ==========

    private String puesto;
    private String indicador;
    private Integer conceptoDetalle;
    private String sucursal;
    private Integer negocio;

    // ========== DATOS OBSERVADOS ==========

    /**
     * Valor real observado en el período actual
     */
    @JsonProperty("observacionReal")
    private Double observacionReal;

    /**
     * Período actual
     */
    private String periodo;

    // ========== PRONÓSTICO ARIMA ==========

    /**
     * Valor pronosticado por el modelo
     */
    private Double rangoPrediccion;

    /**
     * Límite inferior del intervalo de predicción 95%
     */
    @JsonProperty("limiteInferior")
    private Double limiteInferior;

    /**
     * Límite superior del intervalo de predicción 95%
     */
    @JsonProperty("limiteSuperior")
    private Double limiteSuperior;

    /**
     * Error estándar del pronóstico
     */
    @JsonProperty("error_estandar")
    private Double errorEstandar;

    // ========== MODELO UTILIZADO ==========

    /**
     * Notación del modelo ARIMA(p,d,q)
     */
    @JsonProperty("modelo")
    private String modeloNotacion;

    /**
     * AIC del modelo
     */
    private Double aic;

    /**
     * Número de observaciones históricas utilizadas
     */
    @JsonProperty("n_observaciones")
    private Integer numeroObservaciones;

    // ========== ALERTA ==========

    /**
     * ¿El valor observado está fuera del intervalo?
     * TRUE si observación < LI o observación > LS
     */
    @JsonProperty("fuera_de_rango")
    private Boolean fueraDeRango;

    /**
     * Variación porcentual respecto al pronóstico
     * ((Observación - Pronóstico) / Pronóstico) × 100
     */
    @JsonProperty("variacionFueraDelRango")
    private Double variacionFueraDelRango;

    /**
     * Severidad de la alerta
     * - "NORMAL": Dentro del intervalo
     * - "ALERTA": Fuera del intervalo
     */
    private String severidad;

    /**
     * Color para visualización
     */
    @JsonProperty("color_severidad")
    private String colorSeveridad;

    /**
     * Distancia en errores estándar desde el pronóstico
     * (similar a Z-Score)
     */
    @JsonProperty("distancia_se")
    private Double distanciaSE;

    /**
     * Ancho del intervalo de predicción
     */
    @JsonProperty("ancho_intervalo")
    private Double anchoIntervalo;

    /**
     * Redondea a N decimales
     */
    private static double round(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /**
     * 🔴 CONSTRUCTOR PRINCIPAL
     *
     * Según PDF: Alerta si observación ∉ [LI, LS]
     */
    public static ArimaResult crear(
            String puesto, String indicador, Integer conceptoDetalle,
            String sucursal, Integer negocio, String periodo,
            double observacionReal, ArimaForecast forecast,
            ArimaModel modelo) {

        // Verificar si está fuera de rango
        boolean fuera = observacionReal < forecast.getLimiteInferior() ||
                observacionReal > forecast.getLimiteSuperior();

        // Calcular variación porcentual
        double variacion = 0;
        if (forecast.getPronostico() != 0) {
            variacion = ((observacionReal - forecast.getPronostico()) /
                    forecast.getPronostico()) * 100;
        }

        // Calcular distancia en SE (similar a Z-Score)
        double distancia = 0;
        if (forecast.getErrorEstandar() > 0) {
            distancia = Math.abs(observacionReal - forecast.getPronostico()) /
                    forecast.getErrorEstandar();
        }

        // Determinar severidad
        String severidad = fuera ? "ALERTA" : "NORMAL";
        String color = fuera ? "#F44336" : "#4CAF50";  // Rojo : Verde

        // Ancho del intervalo
        double ancho = forecast.getLimiteSuperior() - forecast.getLimiteInferior();

        /*
        *  "puesto": "AC Movilidad",
                "indicador": "Venta con Préstamos",
                "conceptoDetalle": 1002,
                "sucursal": "Elektra",
                "negocio": 1,
                "observacionReal": 0,
                "limiteInferior": 4026066.87,
                "limiteSuperior": 16947316.62,
                "rangoPrediccion": 12921249.75,
                "variacionFueraDelRango": 100,
                "direccionDesviacion": "INFERIOR",
                "fueraDeRango": true,
                "cantidadPeriodosHistoricos": 148,
                "modeloRobusto": true,
                "advertencia": null
        * */


        return ArimaResult.builder()
                .puesto(puesto != null ? puesto.trim() : "")
                .indicador(indicador != null ? indicador.trim() : "")
                .conceptoDetalle(conceptoDetalle)
                .sucursal(sucursal != null ? sucursal.trim() : "")
                .negocio(negocio)
                .observacionReal(round(variacion, 2))
                .limiteInferior(round(forecast.getLimiteInferior(), 2)*100)
                .limiteSuperior(round(forecast.getLimiteSuperior(), 2)*100)
                .rangoPrediccion(round(forecast.getPronostico(), 2))//RANGOPREDICCION
                .variacionFueraDelRango(round(observacionReal, 2)*100)
                .severidad(severidad)
                .fueraDeRango(fuera)
                .numeroObservaciones(modelo.getNumeroObservaciones())
                .distanciaSE(round(distancia, 2))
                .periodo(periodo)
                .errorEstandar(round(forecast.getErrorEstandar(), 4))
                .modeloNotacion(modelo.getNotacion())
                .aic(round(modelo.getAic(), 2))
                .colorSeveridad(color)
                .anchoIntervalo(round(ancho, 2))
                .build();





    }
}