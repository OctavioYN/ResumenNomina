package com.resumen.nomina.alertas.zscore.application.service;


import com.resumen.nomina.alertas.shared.domain.AlertSeverity;
import com.resumen.nomina.alertas.zscore.application.util.ZScoreNumberHelper;
import com.resumen.nomina.alertas.zscore.domain.model.*;
        import com.resumen.nomina.alertas.zscore.domain.repository.ZScoreCalculatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 🧮 SERVICIO PRINCIPAL DE CÁLCULO Z-SCORE
 * Orquesta todo el proceso según requerimientos PDF
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZScoreCalculationService {

    private final ZScoreCalculatorRepository calculatorRepository;
    private final AdaptiveThresholdService thresholdService;

    /**
     * Ejecuta cálculo completo de alertas Z-Score
     */
    public ZScoreCalculationResponse calcularAlertas(String periodoActual, String sucursal,
                                                     Integer negocio, ZScoreConfig config) {
        log.info("🚀 Iniciando cálculo Z-Score - Período: {}, Sucursal: {}, Config: {}",
                periodoActual, sucursal, config.getCodigoConfiguracion());

        try {
            // 1. Validar configuración
            config.validar();

            // 2. Obtener estadísticas históricas
            var estadisticas = calculatorRepository.calcularEstadisticasHistoricas(
                    periodoActual, sucursal, negocio, config);

            // 3. Obtener datos actuales
            var datosActuales = calculatorRepository.obtenerDatosActuales(
                    periodoActual, sucursal, negocio, config);

            // 4. Calcular resultados para cada puesto/indicador
            List<ZScoreResult> resultados = calcularResultados(
                    estadisticas, datosActuales, periodoActual, config);

            // 5. Generar resumen
            var resumen = generarResumen(resultados);

            // 6. Convertir a formato visual
            var resultadosVisuales = resultados.stream()
                    .map(ZScoreResult::toVisual)
                    .collect(Collectors.toList());

            log.info("✅ Cálculo Z-Score completado - Total evaluados: {}, Alertas activas: {}",
                    resultados.size(), resumen.getAlertasActivas());

            return ZScoreCalculationResponse.builder()
                    .success(true)
                    .periodoActual(periodoActual)
                    .sucursal(sucursal != null ? sucursal : "TODAS")
                    .configuracionUsada(config.getCodigoConfiguracion())
                    .resumen(resumen)
                    .resultados(resultadosVisuales)
                    .totalEvaluados(resultados.size())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error en cálculo Z-Score: {}", e.getMessage(), e);
            return ZScoreCalculationResponse.crearError("Error en cálculo Z-Score: " + e.getMessage());
        }
    }

    /**
     * Calcula resultados individuales para cada combinación puesto/indicador
     */
    private List<ZScoreResult> calcularResultados(
            ZScoreCalculatorRepository.EstadisticasHistoricas estadisticas,
            List<ZScoreData> datosActuales, String periodoActual, ZScoreConfig config) {

        List<ZScoreResult> resultados = new ArrayList<>();

        for (var estadistica : estadisticas.getEstadisticas()) {
            try {
                // Buscar dato actual correspondiente
                Optional<ZScoreData> datoActualOpt = datosActuales.stream()
                        .filter(d -> coincidePuestoIndicador(d, estadistica))
                        .findFirst();

                if (datoActualOpt.isPresent()) {
                    ZScoreData datoActual = datoActualOpt.get();
                    ZScoreResult resultado = calcularResultadoIndividual(estadistica, datoActual, periodoActual, config);
                    resultados.add(resultado);
                } else {
                    log.debug("⚠️ No se encontró dato actual para {}-{}",
                            estadistica.getPuesto(), estadistica.getIndicador());
                }

            } catch (Exception e) {
                log.error("❌ Error calculando resultado para {}-{}: {}",
                        estadistica.getPuesto(), estadistica.getIndicador(), e.getMessage());
            }
        }

        return resultados;
    }

    /**
     * Calcula resultado individual para un puesto/indicador
     */
    private ZScoreResult calcularResultadoIndividual(
            ZScoreCalculatorRepository.EstadisticasHistoricas.EstadisticaPuesto estadistica,
            ZScoreData datoActual, String periodoActual, ZScoreConfig config) {

        // Normalizar valores a formato decimal
        double variacionMedia = ZScoreNumberHelper.normalizarADecimal(estadistica.getVariacionMedia());
        double desviacionEstandar = ZScoreNumberHelper.normalizarADecimal(estadistica.getDesviacionEstandar());
        double variacionActual = ZScoreNumberHelper.normalizarADecimal(datoActual.getVariacion());

        // Calcular Z-Score
        double zScore = ZScoreNumberHelper.calcularZScore(variacionActual, variacionMedia, desviacionEstandar);

        // Calcular límites adaptativos
        var limites = thresholdService.calcularLimitesCompletos(variacionMedia, desviacionEstandar, config);

        // Aplicar validación triple
        boolean alertaActiva = config.isUsarValidacionTriple() ?
                thresholdService.validarCondicionesAlerta(variacionActual, zScore, variacionMedia, desviacionEstandar, config) :
                (variacionActual < limites.getLimiteInferior() || variacionActual > limites.getLimiteSuperior());

        // Determinar severidad
        AlertSeverity severidad = AlertSeverity.fromZScore(
                zScore, config.getUmbralCritico(), config.getUmbralAlto(), config.getUmbralModerado());

        // Validar datos suficientes
        boolean datosSuficientes = estadistica.getCantidadPeriodos() >= config.getPeriodosMinimosHistoricos();
        String mensajeAdvertencia = datosSuficientes ? null : "Datos históricos insuficientes";

        log.debug("📊 Resultado {}-{}: Variación: {:.2f}%, Z: {:.2f}, Severidad: {}, Alerta: {}",
                estadistica.getPuesto(), estadistica.getIndicador(),
                variacionActual * 100, zScore, severidad, alertaActiva);

        return ZScoreResult.builder()
                .puesto(estadistica.getPuesto())
                .indicador(estadistica.getIndicador())
                .conceptoDetalle(estadistica.getConceptoDetalle())
                .sucursal(estadistica.getSucursal())
                .negocio(estadistica.getNegocio())
                .periodoEvaluado(periodoActual)
                .variacionActual(variacionActual)
                .variacionMedia(variacionMedia)
                .desviacionEstandar(desviacionEstandar)
                .zScore(zScore)
                .limiteInferior(limites.getLimiteInferior())
                .limiteSuperior(limites.getLimiteSuperior())
                .margenAdaptativo(limites.getMargenAdaptativo())
                .superaUmbrales(variacionActual < limites.getLimiteInferior() || variacionActual > limites.getLimiteSuperior())
                .diferenciaSignificativa(Math.abs(variacionActual) > config.getUmbralDiferenciaMinima())
                .zScoreSignificativo(Math.abs(zScore) > config.getUmbralZScoreMinimo())
                .alertaActiva(alertaActiva)
                .severidad(severidad)
                .colorSeveridad(severidad.getColorHex())
                .emojiSeveridad(severidad.getEmoji())
                .cantidadPeriodosHistoricos(estadistica.getCantidadPeriodos())
                .datosSuficientes(datosSuficientes)
                .mensajeAdvertencia(mensajeAdvertencia)
                .build();
    }

    /**
     * Genera resumen estadístico de los resultados
     */
    private ZScoreSummary generarResumen(List<ZScoreResult> resultados) {
        long totalAlertas = resultados.stream().filter(ZScoreResult::isAlertaActiva).count();
        long criticas = resultados.stream().filter(r -> r.getSeveridad() == AlertSeverity.CRITICA).count();
        long altas = resultados.stream().filter(r -> r.getSeveridad() == AlertSeverity.ALTA).count();
        long moderadas = resultados.stream().filter(r -> r.getSeveridad() == AlertSeverity.MODERADA).count();
        long normales = resultados.stream().filter(r -> r.getSeveridad() == AlertSeverity.NORMAL).count();

        double porcentajeAlertas = resultados.isEmpty() ? 0.0 : (totalAlertas * 100.0) / resultados.size();

        return ZScoreSummary.builder()
                .totalEvaluados(resultados.size())
                .alertasActivas((int) totalAlertas)
                .alertasCriticas((int) criticas)
                .alertasAltas((int) altas)
                .alertasModeradas((int) moderadas)
                .alertasNormales((int) normales)
                .porcentajeAlertas(ZScoreNumberHelper.redondear(porcentajeAlertas, 1))
                .build();
    }

    private boolean coincidePuestoIndicador(ZScoreData dato,
                                            ZScoreCalculatorRepository.EstadisticasHistoricas.EstadisticaPuesto estadistica) {
        return dato.getPuesto().equals(estadistica.getPuesto()) &&
                dato.getIndicador().equals(estadistica.getIndicador()) &&
                dato.getConceptoDetalle().equals(estadistica.getConceptoDetalle()) &&
                dato.getSucursal().equals(estadistica.getSucursal()) &&
                dato.getNegocio().equals(estadistica.getNegocio());
    }
}