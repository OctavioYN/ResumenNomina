package com.resumen.nomina.alertas.arima.application.service;

import com.resumen.nomina.alertas.arima.domain.model.*;
import com.resumen.nomina.alertas.arima.domain.repository.ArimaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🧮 SERVICIO PRINCIPAL ARIMA
 *
 * Implementa detección de outliers usando modelo ARIMA
 * con intervalo de predicción del 95%
 *
 * Según PDF: "Alertas de compensación - Modelo ARIMA"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArimaService {

    private final ArimaRepository repository;
    private final ArimaModelService modelService;

    /**
     * 🎯 MÉTODO PRINCIPAL
     *
     * Calcula alertas ARIMA:
     * 1. Obtiene series temporales históricas
     * 2. Para cada serie, ajusta modelo ARIMA
     * 3. Genera pronóstico con intervalo del 95%
     * 4. Compara observación real con intervalo
     * 5. Genera alerta si está fuera del rango
     */
    public ArimaResponse calcularAlertas(String periodo, String sucursal) {

        log.info("🚀 Iniciando cálculo ARIMA - Período: {}, Sucursal: {}",
                periodo, sucursal != null ? sucursal : "TODAS");

        try {
            ArimaConfig config = ArimaConfig.porDefecto();

            // 1. Obtener series temporales históricas (excluyendo período actual)
            Map<String, List<ArimaData>> seriesTemporales =
                    repository.obtenerSeriesTemporales(periodo, sucursal, config);

            log.info("📊 Series temporales obtenidas: {}", seriesTemporales.size());

            // 2. Obtener datos del período actual
            List<ArimaData> datosActuales =
                    repository.obtenerDatosActuales(periodo, sucursal, config);

            log.info("📋 Datos actuales: {}", datosActuales.size());

            // Crear mapa de datos actuales para búsqueda rápida
            // Si hay duplicados, promediar los valores
            Map<String, ArimaData> mapaActuales = datosActuales.stream()
                    .collect(Collectors.toMap(
                            ArimaData::getClave,
                            d -> d,
                            (d1, d2) -> {
                                // Promediar valores duplicados
                                log.warn("⚠️ Duplicado detectado: {} - Promediando valores: {} y {}",
                                        d1.getClave().substring(0, Math.min(50, d1.getClave().length())),
                                        String.format("%.4f", d1.getValor()),
                                        String.format("%.4f", d2.getValor()));

                                double valorPromedio = (d1.getValor() + d2.getValor()) / 2.0;

                                return ArimaData.builder()
                                        .puesto(d1.getPuesto())
                                        .indicador(d1.getIndicador())
                                        .conceptoDetalle(d1.getConceptoDetalle())
                                        .sucursal(d1.getSucursal())
                                        .negocio(d1.getNegocio())
                                        .periodo(d1.getPeriodo())
                                        .valor(valorPromedio)
                                        .build();
                            }
                    ));

            // 3. Calcular alertas para cada serie
            List<ArimaResult> resultados = new ArrayList<>();
            int sinDatoActual = 0;
            int modelosInvalidos = 0;

            for (Map.Entry<String, List<ArimaData>> entry : seriesTemporales.entrySet()) {
                String clave = entry.getKey();
                List<ArimaData> serieHistorica = entry.getValue();

                // Obtener información del primer elemento (todos comparten identificadores)
                ArimaData primerDato = serieHistorica.get(0);

                // Buscar dato actual correspondiente
                ArimaData datoActual = mapaActuales.get(clave);

                if (datoActual == null) {
                    log.debug("🔴 Sin dato actual: {}-{}",
                            truncate(primerDato.getPuesto(), 20),
                            truncate(primerDato.getIndicador(), 30));
                    sinDatoActual++;
                    continue;
                }

                try {
                    // Extraer valores de la serie
                    List<Double> valores = serieHistorica.stream()
                            .map(ArimaData::getValor)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // Validar serie
                    if (valores.size() < config.getPeriodosMinimos()) {
                        log.debug("⚠️ Serie muy corta: {} observaciones (mínimo: {})",
                                valores.size(), config.getPeriodosMinimos());
                        modelosInvalidos++;
                        continue;
                    }

                    // 4. Ajustar modelo ARIMA y generar pronóstico
                    ArimaForecast forecast = modelService.ajustarYPronosticar(valores, config);

                    // 5. Crear resultado comparando con observación real
                    ArimaResult resultado = ArimaResult.crear(
                            primerDato.getPuesto(),
                            primerDato.getIndicador(),
                            primerDato.getConceptoDetalle(),
                            primerDato.getSucursal(),
                            primerDato.getNegocio(),
                            periodo,
                            datoActual.getValor(),
                            forecast,
                            crearModeloSimplificado(valores.size()) // Modelo placeholder
                    );

                    resultados.add(resultado);

                    // Log de alertas detectadas
                    if (resultado.getFueraDeRango()) {
                        log.debug("🚨 ALERTA: {}-{} | Obs: {}, Intervalo: [{}, {}]",
                                truncate(resultado.getPuesto(), 20),
                                truncate(resultado.getIndicador(), 30),
                                String.format("%.2f", resultado.getObservacionReal()),
                                String.format("%.2f", resultado.getLimiteInferior()),
                                String.format("%.2f", resultado.getLimiteSuperior()));
                    }

                } catch (Exception e) {
                    log.warn("❌ Error procesando serie {}-{}: {}",
                            truncate(primerDato.getPuesto(), 20),
                            truncate(primerDato.getIndicador(), 30),
                            e.getMessage());
                    modelosInvalidos++;
                }
            }

            // 6. Generar resumen
            if (sinDatoActual > 0) {
                log.warn("⚠️ SIN DATO ACTUAL: {} series sin datos del período {}",
                        sinDatoActual, periodo);
            }

            if (modelosInvalidos > 0) {
                log.warn("⚠️ MODELOS INVÁLIDOS: {} series no pudieron ser modeladas",
                        modelosInvalidos);
            }

            long normales = resultados.stream()
                    .filter(r -> "NORMAL".equals(r.getSeveridad())).count();
            long alertas = resultados.stream()
                    .filter(r -> "ALERTA".equals(r.getSeveridad())).count();

            log.info("✅ Completado - Total: {}, Normal: {}, Alertas: {}, Sin dato: {}, Inválidos: {}",
                    resultados.size(), normales, alertas, sinDatoActual, modelosInvalidos);

            // Análisis de resultados
            if (resultados.size() > 0) {
                double pctAlertas = (alertas * 100.0) / resultados.size();
                log.info("📊 ANÁLISIS: {}% fuera del intervalo de predicción 95%",
                        String.format("%.1f", pctAlertas));

                if (pctAlertas > 10) {
                    log.warn("⚠️ Más del 10% son alertas - Revisar calidad de modelos");
                }
            }

            return ArimaResponse.exito(periodo, sucursal, resultados,
                    sinDatoActual, modelosInvalidos);

        } catch (Exception e) {
            log.error("❌ Error calculando alertas ARIMA: {}", e.getMessage(), e);
            return ArimaResponse.error("Error: " + e.getMessage());
        }
    }

    /**
     * Crea un modelo simplificado para el resultado
     * (el modelo completo se genera internamente en ArimaModelService)
     */
    private ArimaModel crearModeloSimplificado(int numObservaciones) {
        return ArimaModel.builder()
                .p(1)
                .d(0)
                .q(0)
                .aic(0)
                .bic(0)
                .errorEstandar(0)
                .numeroObservaciones(numObservaciones)
                .build();
    }

    /**
     * Trunca string para logs
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        String trimmed = str.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}