package com.resumen.nomina.alertas.arima.presentation.controller;


import com.resumen.nomina.alertas.arima.application.service.ArimaService;
import com.resumen.nomina.alertas.arima.domain.model.ArimaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 🌐 REST CONTROLLER ARIMA
 *
 * Endpoints para detección de outliers usando modelo ARIMA
 * con intervalo de predicción del 95%
 */
@Slf4j
@RestController
//@RequestMapping("/api/alertas/arima")
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArimaController {

    private final ArimaService service;

    /**
     * GET /api/alertas/arima/calcular
     *
     * Calcula alertas ARIMA usando intervalo de predicción del 95%
     *
     * @param periodoActual Período a analizar (ej: 202544)
     * @param sucursal Filtro de sucursal (opcional)
     * @return Alertas detectadas
     */
    @GetMapping("/arima")
    public ResponseEntity<ArimaResponse> calcular(
            @RequestParam(defaultValue = "202544") String periodoActual,
            @RequestParam(required = false) String sucursal) {

        log.info("🎯 Solicitud ARIMA - Período: {}, Sucursal: {}",
                periodoActual, sucursal != null ? sucursal : "TODAS");

        try {
            ArimaResponse response = service.calcularAlertas(periodoActual, sucursal);

            // Log de resultados
            if (response.getSuccess() && response.getTotalEvaluados() > 0) {
                double pctAlertas = (response.getAlertasActivas() * 100.0) /
                        response.getTotalEvaluados();

                log.info("📊 RESULTADOS: {}% fuera del intervalo 95%",
                        String.format("%.1f", pctAlertas));

                if (pctAlertas > 10) {
                    log.warn("⚠️ Más del 10% son alertas - Posible problema en datos o modelos");
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error en controller: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ArimaResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/alertas/arima/health
     *
     * Health check del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "ARIMA Alerts",
                "version", "1.0",
                "library", "Apache Commons Math 3"
        ));
    }

    /**
     * GET /api/alertas/arima/info
     *
     * Información sobre el modelo ARIMA implementado
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "documento", "Alertas de compensación - Modelo ARIMA",
                "metodo", "AutoRegressive Integrated Moving Average",
                "intervalo_prediccion", "95% (± 1.96 × SE)",
                "criterio_alerta", "Observación fuera de [LI, LS]",
                "biblioteca", "Apache Commons Math 3.6.1",
                "componentes", Map.of(
                        "AR", "Componente autorregresivo (p)",
                        "I", "Diferenciación (d) para estacionariedad",
                        "MA", "Promedio móvil (q)"
                ),
                "seleccion_modelo", Map.of(
                        "metodo", "Grid search sobre (p,d,q)",
                        "criterio", "AIC (Akaike Information Criterion)",
                        "rangos", "p ∈ [0,3], d ∈ [0,2], q ∈ [0,3]"
                ),
                "ventajas", List.of(
                        "Captura tendencias y patrones temporales",
                        "Adaptativo al comportamiento histórico",
                        "Intervalo de confianza estadísticamente robusto",
                        "Implementación Java pura (sin dependencias externas)"
                ),
                "nota", "Para series con < 12 observaciones, el modelo puede ser inestable"
        ));
    }

    /**
     * GET /api/alertas/arima/comparacion
     *
     * Información comparativa entre Z-Score y ARIMA
     */
    @GetMapping("/comparacion")
    public ResponseEntity<Map<String, Object>> comparacion() {
        return ResponseEntity.ok(Map.of(
                "titulo", "Z-Score vs ARIMA",
                "z_score", Map.of(
                        "metodo", "Desviación estándar de variaciones históricas",
                        "supuestos", "Variaciones siguen distribución aproximadamente normal",
                        "ventaja", "Más simple, rápido de calcular",
                        "desventaja", "No captura patrones temporales (tendencias, ciclos)"
                ),
                "arima", Map.of(
                        "metodo", "Modelo autorregresivo con diferenciación y media móvil",
                        "supuestos", "Serie puede ser estacionaria tras diferenciación",
                        "ventaja", "Captura dependencia temporal, tendencias y estacionalidad",
                        "desventaja", "Más complejo, requiere más datos (≥12 períodos)"
                ),
                "cuando_usar", Map.of(
                        "z_score", "Series estables sin tendencia clara, alertas rápidas",
                        "arima", "Series con tendencia/estacionalidad, pronósticos más precisos"
                ),
                "complementariedad", "Se recomienda usar ambos métodos y comparar resultados"
        ));
    }
}