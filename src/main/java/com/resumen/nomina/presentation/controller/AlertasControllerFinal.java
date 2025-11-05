package com.resumen.nomina.presentation.controller;

import com.resumen.nomina.application.service.AlertasServiceOptimizado;
import com.resumen.nomina.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 🚨 CONTROLLER OPTIMIZADO DE ALERTAS
 * Endpoints que devuelven exactamente la estructura de las imágenes
 */
@Slf4j
@RestController
@RequestMapping("/api/alertas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlertasControllerFinal {

    private final AlertasServiceOptimizado alertasService;

    /**
     * 📊 GET /api/alertas/zscore
     * Devuelve alertas Z-Score según Imagen 1
     *
     * Estructura de respuesta:
     * {
     *   "success": true,
     *   "tipo": "Z_SCORE",
     *   "titulo": "Z-Score",
     *   "descripcion": "...",
     *   "consideraciones": [...],
     *   "periodoActual": "202540",
     *   "resumen": { ... },
     *   "alertas": [
     *     {
     *       "puesto": "Vendedor",
     *       "indicador": "Colocación",
     *       "variacionPorcentualVsSA": 70.80,
     *       "variacionMedia": 1.44,
     *       "limiteInferior": -14.29,
     *       "limiteSuperior": 17.16,
     *       "severidad": "CRITICA",
     *       "colorSeveridad": "#F44336"
     *     }
     *   ]
     * }
     */
    /**
     * 📊 GET /api/alertas/zscore
     * Ahora acepta parámetro de configuración
     */
    @GetMapping("/zscore")
    public ResponseEntity<Map<String, Object>> obtenerAlertasZScore(
            @RequestParam(defaultValue = "202540") String periodoActual,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Integer negocio,
            @RequestParam(defaultValue = "DEFAULT") String configuracion) {  // ← NUEVO

        log.info("📊 GET /zscore - Período: {}, Config: {}", periodoActual, configuracion);

        try {
            AlertasZScoreResponse response = alertasService.obtenerAlertasZScore(
                    periodoActual, sucursal, negocio, configuracion);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("configuracionUsada", configuracion);  // ← Informar qué config se usó
            resultado.putAll(convertirZScoreAMap(response));

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en Z-Score: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Error procesando alertas Z-Score",
                    "mensaje", e.getMessage()
            ));
        }
    }
    /**
     * 📈 GET /api/alertas/arima
     * Devuelve alertas ARIMA según Imagen 2
     *
     * Estructura de respuesta:
     * {
     *   "success": true,
     *   "tipo": "ARIMA",
     *   "titulo": "Intervalo de Predicción 95%",
     *   "descripcion": "...",
     *   "advertencia": "*Puestos sin suficiente historia...",
     *   "periodoActual": "202540",
     *   "resumen": { ... },
     *   "alertas": [
     *     {
     *       "puesto": "AdP SF Maduro",
     *       "indicador": "Intereses BF",
     *       "observacionReal": 107238,
     *       "limiteInferior": 98052,
     *       "limiteSuperior": 106672,
     *       "variacionFueraDelRango": 1
     *     }
     *   ],
     *   "modelosNoRobustos": [...]
     * }
     */

    /**
     * 📈 GET /api/alertas/arima
     */
    @GetMapping("/arima")
    public ResponseEntity<Map<String, Object>> obtenerAlertasARIMA(
            @RequestParam(defaultValue = "202540") String periodoActual,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Integer negocio,
            @RequestParam(defaultValue = "DEFAULT") String configuracion) {  // ← NUEVO

        log.info("📈 GET /arima - Período: {}, Config: {}", periodoActual, configuracion);

        try {
            AlertasARIMAResponse response = alertasService.obtenerAlertasARIMA(
                    periodoActual, sucursal, negocio, configuracion);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("configuracionUsada", configuracion);
            resultado.putAll(convertirARIMAMap(response));

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en ARIMA: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Error procesando alertas ARIMA",
                    "mensaje", e.getMessage()
            ));
        }
    }



    /**
     * 📋 GET /api/alertas/completas
     * Devuelve ambos sistemas de alertas
     */
    /**
     * 📋 GET /api/alertas/completas
     */
    @GetMapping("/completas")
    public ResponseEntity<Map<String, Object>> obtenerAlertasCompletas(
            @RequestParam(defaultValue = "202540") String periodoActual,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Integer negocio,
            @RequestParam(defaultValue = "DEFAULT") String configuracion,  // ← NUEVO
            @RequestParam(defaultValue = "false") boolean guardarHistorial,
            @RequestParam(defaultValue = "sistema") String usuario) {

        log.info("📋 GET /completas - Período: {}, Config: {}", periodoActual, configuracion);

        try {
            AlertasCompletasResponse response = alertasService.obtenerAlertasCompletas(
                    periodoActual, sucursal, negocio, configuracion, guardarHistorial, usuario);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("configuracionUsada", configuracion);
            resultado.put("periodoActual", response.getPeriodoActual());
            resultado.put("sucursal", response.getSucursal());
            resultado.put("zscore", convertirZScoreAMap(response.getZscoreResponse()));
            resultado.put("arima", convertirARIMAMap(response.getArimaResponse()));
            resultado.put("resumenGeneral", response.getResumenGeneral());
            resultado.put("fechaGeneracion", response.getFechaGeneracion());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en alertas completas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Error procesando alertas completas",
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * 🎯 GET /api/alertas/criticas
     * Filtra solo alertas críticas
     */
    @GetMapping("/criticas")
    public ResponseEntity<Map<String, Object>> obtenerAlertasCriticas(
            @RequestParam(defaultValue = "202540") String periodoActual,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Integer negocio,
            @RequestParam(defaultValue = "DEFAULT") String configuracion ) {

        log.info("🎯 GET /criticas - Período: {}", periodoActual);

        try {
            AlertasZScoreResponse response = alertasService.obtenerAlertasZScore(
                    periodoActual, sucursal, negocio,configuracion);

            List<AlertaZScoreDTO> criticas = response.getAlertas().stream()
                    .filter(a -> a.getSeveridad() == SeveridadAlerta.CRITICA)
                    .toList();

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("tipo", "ALERTAS_CRITICAS");
            resultado.put("periodoActual", periodoActual);
            resultado.put("total", criticas.size());
            resultado.put("alertas", criticas);
            resultado.put("mensaje", "Alertas que requieren atención inmediata");

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error obteniendo críticas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 📊 GET /api/alertas/resumen
     * Dashboard con KPIs
     */
    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumen(
            @RequestParam(defaultValue = "202540") String periodoActual,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) Integer negocio,
            @RequestParam(defaultValue = "DEFAULT") String configuracion

    ) {

        log.info("📊 GET /resumen - Período: {}", periodoActual);

        try {
            AlertasCompletasResponse response = alertasService.obtenerAlertasCompletas(
                    periodoActual, sucursal, negocio, configuracion, false, "sistema");

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("success", true);
            dashboard.put("periodoActual", periodoActual);
            dashboard.put("resumenZScore", response.getZscoreResponse().getResumen());
            dashboard.put("resumenARIMA", response.getArimaResponse().getResumen());
            dashboard.put("resumenGeneral", response.getResumenGeneral());
            dashboard.put("fechaConsulta", response.getFechaGeneracion());

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Error en resumen: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private Map<String, Object> convertirZScoreAMap(AlertasZScoreResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("tipo", response.getTipo());
        map.put("titulo", response.getTitulo());
        map.put("descripcion", response.getDescripcion());
        map.put("consideraciones", response.getConsideraciones());
        map.put("periodoActual", response.getPeriodoActual());
        map.put("sucursal", response.getSucursal());
        map.put("resumen", response.getResumen());
        map.put("alertas", response.getAlertas());
        map.put("totalAlertas", response.getAlertas().size());
        map.put("fechaGeneracion", response.getFechaGeneracion());
        return map;
    }

    private Map<String, Object> convertirARIMAMap(AlertasARIMAResponse response) {
        Map<String, Object> map = new HashMap<>();
        map.put("tipo", response.getTipo());
        map.put("titulo", response.getTitulo());
        map.put("descripcion", response.getDescripcion());
        map.put("advertencia", response.getAdvertencia());
        map.put("periodoActual", response.getPeriodoActual());
        map.put("sucursal", response.getSucursal());
        map.put("resumen", response.getResumen());
        map.put("alertas", response.getAlertas());
        map.put("totalAlertas", response.getAlertas().size());
        map.put("modelosRobustos", response.getModelosRobustos());
        map.put("modelosNoRobustos", response.getModelosNoRobustos());
        map.put("fechaGeneracion", response.getFechaGeneracion());
        return map;
    }
}