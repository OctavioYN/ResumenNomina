package com.resumen.nomina.alertas.zscore.application.service;

import com.resumen.nomina.alertas.zscore.domain.model.*;
import com.resumen.nomina.alertas.zscore.domain.repository.ZScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 🧮 SERVICIO PRINCIPAL Z-SCORE - VERSIÓN CORREGIDA
 * Incluye registros sin dato actual con severidad especial
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZScoreService {

    private final ZScoreRepository repository;

    public ZScoreResponse calcularAlertas(String periodo, String sucursal) {

        log.info("🚀 Iniciando cálculo Z-Score - Período: {}, Sucursal: {}",
                periodo, sucursal != null ? sucursal : "TODAS");

        try {
            ZScoreConfig config = ZScoreConfig.porDefecto();

            // 1. Obtener estadísticas históricas (ya normalizadas)
            List<ZScoreRepository.Estadistica> estadisticas =
                    repository.calcularEstadisticas(periodo, sucursal, config);

            log.info("📊 Estadísticas calculadas: {}", estadisticas.size());

            // 2. Obtener datos actuales (ya normalizados)
            List<ZScoreData> datosActuales =
                    repository.obtenerDatosActuales(periodo, sucursal, config);

            log.info("📋 Datos actuales: {}", datosActuales.size());

            // Verificación
            log.info("🔍 VERIFICACIÓN: Estadísticas={}, DatosActuales={}, Esperado coincidencias={}",
                    estadisticas.size(), datosActuales.size(),
                    Math.min(estadisticas.size(), datosActuales.size()));

            // 3. Calcular resultados
            List<ZScoreResult> resultados = new ArrayList<>();
            int sinDatoActual = 0;
            int desviacionCeroAjustada = 0;
            int desviacionAltaAjustada = 0;

            for (ZScoreRepository.Estadistica est : estadisticas) {

                // Buscar dato actual
                Optional<ZScoreData> datoActualOpt = datosActuales.stream()
                        .filter(d -> coincide(d, est))
                        .findFirst();

                // ✅ INCLUIR registros sin dato actual
                if (datoActualOpt.isEmpty()) {
                    String clave = generarClave(est);

                    // Crear resultado con severidad especial
                    ZScoreResult resultado = ZScoreResult.builder()
                            .puesto(est.getPuesto())
                            .indicador(est.getIndicador())
                            .conceptoDetalle(est.getConceptoDetalle())
                            .sucursal(est.getSucursal())
                            .negocio(est.getNegocio())
                            .variacionPorcentualVsSA(null)  // No hay dato actual
                            .variacionMedia(est.getMedia())
                            .desviacionEstandar(est.getDesviacion())
                            .limiteInferior(null)
                            .limiteSuperior(null)
                            .zscore(null)
                            .zscoreAbsoluto(null)
                            .severidad("SIN_DATO_ACTUAL")
                            .colorSeveridad("#9E9E9E")  // Gris
                            .fueraDeRango(true)  // Marcado como que requiere atención
                            .cantidadPeriodosHistoricos(est.getCantidad())
                            .build();

                    resultados.add(resultado);
                    sinDatoActual++;
                    log.debug("🔴 Sin dato actual: {}", clave);
                    continue;
                }

                ZScoreData datoActual = datoActualOpt.get();

                // ✅ Los valores YA VIENEN normalizados de MongoDB
                double variacionActual = datoActual.getVariacion();
                double media = est.getMedia();
                double desviacion = est.getDesviacion();

                // ✅ MANEJO DE DESVIACIÓN = 0
                if (desviacion == 0 || Double.isNaN(desviacion)) {
                    log.debug("⚪ Desviación=0 para {}-{}, usando σ mínimo de 1%",
                            truncate(est.getPuesto(), 20),
                            truncate(est.getIndicador(), 30));
                    desviacion = 0.01;  // 1% mínimo
                    desviacionCeroAjustada++;
                }

                // ✅ MANEJO DE DESVIACIÓN MUY ALTA
                if (desviacion > 2.0) {  // > 200%
                    log.debug("🔶 Desviación muy alta ({}) para {}-{}, ajustando a 200%",
                            String.format("%.2f%%", desviacion * 100),
                            truncate(est.getPuesto(), 20),
                            truncate(est.getIndicador(), 30));
                    desviacion = 2.0;  // Máximo 200%
                    desviacionAltaAjustada++;
                }




                // Calcular límites
                double margen = calcularMargenAdaptativo(desviacion, config);
                double limInf = media - margen;
                double limSup = media + margen;

                // Crear resultado
                ZScoreResult resultado = ZScoreResult.crear(
                        est.getPuesto(),
                        est.getIndicador(),
                        est.getConceptoDetalle(),
                        est.getSucursal(),
                        est.getNegocio(),
                        variacionActual,
                        media,
                        desviacion,
                        limInf,
                        limSup,
                        est.getCantidad()
                );

                resultados.add(resultado);
            }

            // 4. Log detallado de procesamiento
            if (sinDatoActual > 0) {
                log.warn("⚠️ SIN DATO ACTUAL: {} registros sin datos del período {} ({}% del total histórico)",
                        sinDatoActual,
                        periodo,
                        String.format("%.1f", sinDatoActual * 100.0 / estadisticas.size()));
            }

            if (desviacionCeroAjustada > 0 || desviacionAltaAjustada > 0) {
                log.info("📊 AJUSTES aplicados:");
                if (desviacionCeroAjustada > 0) {
                    log.info("   - {} con σ=0 ajustados a σ=1%", desviacionCeroAjustada);
                }
                if (desviacionAltaAjustada > 0) {
                    log.info("   - {} con σ>200% ajustados a σ=200%", desviacionAltaAjustada);
                }
            }

            // 5. Generar resumen por severidad
            int total = resultados.size();
            long normales = resultados.stream()
                    .filter(r -> "NORMAL".equals(r.getSeveridad())).count();
            long moderadas = resultados.stream()
                    .filter(r -> "MODERADA".equals(r.getSeveridad())).count();
            long altas = resultados.stream()
                    .filter(r -> "ALTA".equals(r.getSeveridad())).count();
            long criticas = resultados.stream()
                    .filter(r -> "CRITICA".equals(r.getSeveridad())).count();
            long sinDato = resultados.stream()
                    .filter(r -> "SIN_DATO_ACTUAL".equals(r.getSeveridad())).count();
            long fueraRango = resultados.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getFueraDeRango())).count();

            log.info("✅ Completado - Total: {}, Normal: {}, Moderada: {}, Alta: {}, Crítica: {}, Sin Dato: {}, Fuera: {}",
                    total, normales, moderadas, altas, criticas, sinDato, fueraRango);

            // 6. Análisis de calidad
            int procesados = total - (int)sinDato;  // Solo los que tienen datos
            if (procesados > 0) {
                double pctCriticas = (criticas * 100.0) / procesados;
                double pctFuera = (fueraRango * 100.0) / procesados;

                log.info("📊 ANÁLISIS (excluyendo sin dato): {}% críticas, {}% fuera de rango",
                        String.format("%.1f", pctCriticas),
                        String.format("%.1f", pctFuera));

                if (pctCriticas > 20) {
                    log.warn("⚠️ Más del 20% son críticas - Revisar normalización de datos");
                }
            }

            return ZScoreResponse.builder()
                    .success(true)
                    .message("Cálculo completado exitosamente")
                    .periodo(periodo)
                    .sucursal(sucursal != null ? sucursal : "TODAS")
                    .totalEvaluados(total)
                    .alertasNormales((int)normales)
                    .alertasModeradas((int)moderadas)
                    .alertasAltas((int)altas)
                    .alertasCriticas((int)criticas)
                    .fueraDeRango((int)fueraRango)
                    .alertas(resultados)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error calculando alertas: {}", e.getMessage(), e);
            return ZScoreResponse.error("Error: " + e.getMessage());
        }
    }

    /**
     * Calcula margen adaptativo según PDF
     */
    private double calcularMargenAdaptativo(double desviacion, ZScoreConfig config) {
        double margen;

        if (desviacion < config.getUmbralEstable()) {
            margen = config.getFactorEstable() * config.getUmbralEstable();
        } else if (desviacion < config.getUmbralVolatilidad()) {
            margen = config.getFactorMedio() * desviacion;
        } else {
            margen = desviacion;
        }

        // Aplicar límites
        margen = Math.max(margen, config.getMargenMinimo());
        margen = Math.min(margen, config.getMargenMaximo());

        return margen;
    }

    /**
     * Verifica coincidencia - Compara sin espacios
     */
    private boolean coincide(ZScoreData dato, ZScoreRepository.Estadistica est) {
        String datoPuesto = dato.getPuesto() != null ? dato.getPuesto().trim() : "";
        String estPuesto = est.getPuesto() != null ? est.getPuesto().trim() : "";
        String datoIndicador = dato.getIndicador() != null ? dato.getIndicador().trim() : "";
        String estIndicador = est.getIndicador() != null ? est.getIndicador().trim() : "";
        String datoSucursal = dato.getSucursal() != null ? dato.getSucursal().trim() : "";
        String estSucursal = est.getSucursal() != null ? est.getSucursal().trim() : "";

        return datoPuesto.equals(estPuesto) &&
                datoIndicador.equals(estIndicador) &&
                Objects.equals(dato.getConceptoDetalle(), est.getConceptoDetalle()) &&
                datoSucursal.equals(estSucursal) &&
                Objects.equals(dato.getNegocio(), est.getNegocio());
    }

    /**
     * Genera clave descriptiva para logs
     */
    private String generarClave(ZScoreRepository.Estadistica est) {
        return String.format("%s-%s",
                truncate(est.getPuesto(), 20),
                truncate(est.getIndicador(), 30));
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