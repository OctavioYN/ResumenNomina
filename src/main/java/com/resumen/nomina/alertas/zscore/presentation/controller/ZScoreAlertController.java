package com.resumen.nomina.alertas.zscore.presentation.controller;

import com.resumen.nomina.alertas.zscore.application.service.ZScoreCalculationService;
import com.resumen.nomina.alertas.zscore.application.util.ZScoreNumberHelper;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreCalculationResponse;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.HashMap;
import java.util.Map;

/**
 * 🌐 CONTROLLER REST PARA ALERTAS Z-SCORE
 */
@Slf4j
@RestController
@RequestMapping("/api/alertas/zscore")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZScoreAlertController {

    private final ZScoreCalculationService calculationService;

    /**
     * GET /api/alertas/zscore/calcular
     * Endpoint principal para cálculo de alertas Z-Score
     */
    @GetMapping("/calcular")
    public ResponseEntity<Map<String, Object>> calcularAlertas(
            @RequestParam(defaultValue = "202540") String periodo,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Integer negocio,
            @RequestParam(defaultValue = "ZSCORE_DEFAULT") String configuracion) {

        log.info("🎯 Calculando alertas Z-Score - Periodo: {}, Sucursal: {}, Config: {}",
                periodo, sucursal, configuracion);

        try {
            // 🔴 EN PRODUCCIÓN: Obtener configuración desde base de datos
            ZScoreConfig config = obtenerConfiguracion(configuracion);

            ZScoreCalculationResponse response = calculationService.calcularAlertas(
                    periodo, sucursal, negocio, config);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", response.isSuccess());
            resultado.put("tipo", "Z_SCORE");
            resultado.put("titulo", "Sistema de Alertas Z-Score");
            resultado.put("descripcion", "Detección de outliers usando Z-Score con validación triple");
            resultado.put("periodo", response.getPeriodoActual());
            resultado.put("sucursal", response.getSucursal());
            resultado.put("configuracion", response.getConfiguracionUsada());
            resultado.put("resumen", response.getResumen());
            resultado.put("alertas", response.getResultados());
            resultado.put("total", response.getTotalEvaluados());
            resultado.put("fechaCalculo", response.getFechaCalculo());

            if (!response.isSuccess()) {
                resultado.put("error", response.getError());
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en controller Z-Score: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Error procesando solicitud Z-Score",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/alertas/zscore/diagnostico
     * Endpoint para diagnóstico y debugging
     */
    @GetMapping("/diagnostico")
    public ResponseEntity<Map<String, Object>> diagnostico(
            @RequestParam String puesto,
            @RequestParam String indicador,
            @RequestParam Double variacion,
            @RequestParam Double media,
            @RequestParam Double desviacion) {

        log.info("🩺 Diagnóstico Z-Score - {}-{}", puesto, indicador);

        try {
            ZScoreConfig config = ZScoreConfig.crearConfiguracionPorDefecto();

            // Normalizar valores
            double variacionNorm = ZScoreNumberHelper.normalizarADecimal(variacion);
            double mediaNorm = ZScoreNumberHelper.normalizarADecimal(media);
            double desviacionNorm = ZScoreNumberHelper.normalizarADecimal(desviacion);

            double zScore = ZScoreNumberHelper.calcularZScore(variacionNorm, mediaNorm, desviacionNorm);

            Map<String, Object> diagnostico = new HashMap<>();
            diagnostico.put("success", true);
            diagnostico.put("puesto", puesto);
            diagnostico.put("indicador", indicador);
            diagnostico.put("valores_originales", Map.of(
                    "variacion", variacion,
                    "media", media,
                    "desviacion", desviacion
            ));
            diagnostico.put("valores_normalizados", Map.of(
                    "variacion", variacionNorm,
                    "media", mediaNorm,
                    "desviacion", desviacionNorm
            ));
            diagnostico.put("zScore", zScore);
            diagnostico.put("zScore_absoluto", Math.abs(zScore));

            return ResponseEntity.ok(diagnostico);

        } catch (Exception e) {
            log.error("❌ Error en diagnóstico: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/alertas/zscore/configuracion
     * Endpoint para obtener configuración actual
     */
   /* @GetMapping("/configuracion")
    public ResponseEntity<Map<String, Object>> obtenerConfiguracion(
            @RequestParam(defaultValue = "ZSCORE_DEFAULT") String codigo) {

        log.info("⚙️ Obteniendo configuración Z-Score: {}", codigo);

        try {
            ZScoreConfig config = obtenerConfiguracion(codigo);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("configuracion", config);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error obteniendo configuración: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }*/

    // 🔴 EN PRODUCCIÓN: Reemplazar con servicio de configuración real
    private ZScoreConfig obtenerConfiguracion(String codigo) {
        // Por ahora siempre retorna configuración por defecto
        // En producción, buscar desde base de datos
        return ZScoreConfig.crearConfiguracionPorDefecto();
    }
}