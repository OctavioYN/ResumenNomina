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
    @JsonProperty("observacionReal")
    private Double observacionReal;

    private String periodo;

    // ========== PRONÓSTICO ARIMA ==========
    @JsonProperty("rangoPrediccion")
    private Double rangoPrediccion;

    @JsonProperty("limiteInferior")
    private Double limiteInferior;

    @JsonProperty("limiteSuperior")
    private Double limiteSuperior;

    @JsonProperty("variacionFueraDelRango")
    private Double variacionFueraDelRango;

    // ========== NUEVOS CAMPOS SEGÚN PDF ==========
    @JsonProperty("direccionDesviacion")
    private String direccionDesviacion;

    @JsonProperty("fueraDeRango")
    private Boolean fueraDeRango;

    @JsonProperty("cantidadPeriodosHistoricos")
    private Integer cantidadPeriodosHistoricos;

    @JsonProperty("modeloRobusto")
    private Boolean modeloRobusto;

    @JsonProperty("advertencia")
    private String advertencia;

    // ========== CAMPOS EXISTENTES (mantener) ==========
    @JsonProperty("error_estandar")
    private Double errorEstandar;

    @JsonProperty("modelo")
    private String modeloNotacion;

    private Double aic;

    @JsonProperty("n_observaciones")
    private Integer numeroObservaciones;

    private String severidad;

    @JsonProperty("color_severidad")
    private String colorSeveridad;

    @JsonProperty("distancia_se")
    private Double distanciaSE;

    @JsonProperty("ancho_intervalo")
    private Double anchoIntervalo;

    /**
     * 🔴 CONSTRUCTOR PRINCIPAL CORREGIDO
     * Según PDF: Alerta si observación ∉ [LI, LS]
     */
    public static ArimaResult crear(
            String puesto, String indicador, Integer conceptoDetalle,
            String sucursal, Integer negocio, String periodo,
            double observacionReal, ArimaForecast forecast,
            ArimaModel modelo, int periodosHistoricos) {

        // Verificar si está fuera de rango
        boolean fuera = observacionReal < forecast.getLimiteInferior() ||
                observacionReal > forecast.getLimiteSuperior();

        // CALCULAR VARIACIÓN PORCENTUAL CORREGIDA
        double variacion = 0;
        if (!fuera) {
            // Si está dentro del rango, variación = 0%
            variacion = 0;
        } else {
            // Calcular qué tan fuera está del rango más cercano
            if (observacionReal < forecast.getLimiteInferior()) {
                // Está por debajo del límite inferior
                double distancia = forecast.getLimiteInferior() - observacionReal;
                double rangoIntervalo = forecast.getLimiteSuperior() - forecast.getLimiteInferior();
                variacion = -(distancia / (rangoIntervalo + 1e-10)) * 100;
            } else {
                // Está por encima del límite superior
                double distancia = observacionReal - forecast.getLimiteSuperior();
                double rangoIntervalo = forecast.getLimiteSuperior() - forecast.getLimiteInferior();
                variacion = (distancia / (rangoIntervalo + 1e-10)) * 100;
            }
        }

        // LIMITAR VARIACIÓN A UN RANGO RAZONABLE
        variacion = Math.max(Math.min(variacion, 1000), -1000);

        // Determinar dirección de desviación
        String direccion = "DENTRO";
        if (fuera) {
            direccion = observacionReal < forecast.getLimiteInferior() ? "INFERIOR" : "SUPERIOR";
        }

        // Determinar si el modelo es robusto
        boolean esRobusto = periodosHistoricos >= 12;
        String advertencia = esRobusto ? null : "Puesto sin suficiente historia. Por lo tanto, este modelo no es robusto en ellos.";

        // Determinar severidad
        String severidad = fuera ? "ALERTA" : "NORMAL";
        String color = fuera ? "#F44336" : "#4CAF50";

        return ArimaResult.builder()
                .puesto(puesto != null ? puesto.trim() : "")
                .indicador(indicador != null ? indicador.trim() : "")
                .conceptoDetalle(conceptoDetalle)
                .sucursal(sucursal != null ? sucursal.trim() : "")
                .negocio(negocio)
                .observacionReal(round(observacionReal, 2))
                .limiteInferior(round(forecast.getLimiteInferior(), 2))
                .limiteSuperior(round(forecast.getLimiteSuperior(), 2))
                .rangoPrediccion(round(forecast.getPronostico(), 2))
                .variacionFueraDelRango(round(variacion, 1)) // 1 decimal como en el ejemplo
                .direccionDesviacion(direccion)
                .fueraDeRango(fuera)
                .cantidadPeriodosHistoricos(periodosHistoricos)
                .modeloRobusto(esRobusto)
                .advertencia(advertencia)
                .severidad(severidad)
                .periodo(periodo)
                .errorEstandar(round(forecast.getErrorEstandar(), 4))
                .modeloNotacion(modelo.getNotacion())
                .aic(round(modelo.getAic(), 2))
                .numeroObservaciones(modelo.getNumeroObservaciones())
                .colorSeveridad(color)
                .build();
    }

    private static double round(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}