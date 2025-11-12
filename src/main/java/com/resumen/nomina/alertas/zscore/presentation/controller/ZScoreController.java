package com.resumen.nomina.alertas.zscore.presentation.controller;

import com.resumen.nomina.alertas.zscore.application.service.ZScoreService;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🌐 REST CONTROLLER Z-SCORE - SEGÚN PDF ORIGINAL
 *
 * Documento: "Alertas de compensación - Z-Score"
 */
@Slf4j
@RestController
//@RequestMapping("/api/alertas/zscore")
@RequestMapping("/api/alertasn")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZScoreController {

    private final ZScoreService service;

    /**
     * GET /api/alertas/zscore/calcular
     *
     * Calcula alertas Z-Score usando la configuración por defecto del PDF
     *
     * @param periodoActual Período a analizar (ej: 202544)
     * @param sucursal Filtro de sucursal (opcional)
     */
    //@GetMapping("/calcular")
    @GetMapping("/zscore")
    public ResponseEntity<ZScoreResponse> calcular(
            @RequestParam(defaultValue = "202544") String periodoActual,
            @RequestParam(required = false) String sucursal) {

        log.info("🎯 Solicitud Z-Score - Período: {}, Sucursal: {}",
                periodoActual, sucursal != null ? sucursal : "TODAS");

        try {
            ZScoreResponse response = service.calcularAlertas(periodoActual, sucursal);

            // Log de resultados
            if (response.getSuccess() && response.getTotalEvaluados() > 0) {
                int conDatos = response.getTotalEvaluados() -
                        (int) response.getAlertas().stream()
                                .filter(a -> "SIN_DATO_ACTUAL".equals(a.getSeveridad()))
                                .count();

                if (conDatos > 0) {
                    double pctCriticas = (response.getAlertasCriticas() * 100.0) / conDatos;
                    double pctFueraRango = (response.getFueraDeRango() * 100.0) / conDatos;

                    log.info("📊 RESULTADOS: {}% críticas (Z>3), {}% fuera de rango",
                            String.format("%.1f", pctCriticas),
                            String.format("%.1f", pctFueraRango));
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error en controller: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ZScoreResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/alertas/zscore/health
     */
    @GetMapping("/zscore/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "Z-Score Alerts",
                "version", "1.0-PDF-ORIGINAL"
        ));
    }

    /**
     * GET /api/alertas/zscore/info
     * Información sobre la configuración según el PDF
     */
    @GetMapping("/zscore/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "documento", "Alertas de compensación - Z-Score",
                "configuracion", "Según PDF original",
                "estrategiaAdaptativa", Map.of(
                        "estable_menor_1pct", "margen fijo 1.5%",
                        "media_1_a_5pct", "margen = 1.3 × σ",
                        "alta_mayor_5pct", "margen = σ",
                        "limites", "[1%, 20%]"
                ),
                "validacionTriple", Map.of(
                        "condicion1", "Superar umbrales dinámicos",
                        "condicion2", "Diferencia > 1%",
                        "condicion3", "Z-Score > 1"
                ),
                "severidad", Map.of(
                        "CRITICA", "Z-Score > 3σ (0.3% probabilidad)",
                        "ALTA", "Z-Score > 2σ (5% probabilidad)",
                        "MODERADA", "Z-Score > 1σ (moderadamente inusual)",
                        "NORMAL", "Z-Score ≤ 1σ"
                )
        ));
    }
}