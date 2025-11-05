
package com.resumen.nomina.presentation.controller;

import com.resumen.nomina.application.service.IndicadorPromedioService;
import com.resumen.nomina.domain.model.GraficaIndicadorPromedioResponse;
import com.resumen.nomina.domain.model.IndicadorPromedioResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/indicadores-promedio")
@CrossOrigin(origins = "*")
public class IndicadorPromedioController {

    private static final Logger logger = LoggerFactory.getLogger(IndicadorPromedioController.class);

    private final IndicadorPromedioService indicadorPromedioService;

    @Autowired
    public IndicadorPromedioController(IndicadorPromedioService indicadorPromedioService) {
        this.indicadorPromedioService = indicadorPromedioService;
    }

    /**
     * GET /api/indicadores-promedio/grafica
     * Obtiene datos para gráfica histórica de un indicador específico
     *
     * Ejemplo: GET /api/indicadores-promedio/grafica?negocio=3&puesto=2319&indicador=1001&anioDesde=2023&usuario=admin
     */
    @GetMapping("/grafica")
    public ResponseEntity<?> obtenerGraficaIndicador(
            @RequestParam Integer negocio,
            @RequestParam Integer puesto,
            @RequestParam Integer indicador,
            @RequestParam(defaultValue = "2023") Integer anioDesde,
            @RequestParam(defaultValue = "admin") String usuario) {

        logger.info("GET /api/indicadores-promedio/grafica - Negocio={}, Puesto={}, Indicador={}, Desde={}",
                negocio, puesto, indicador, anioDesde);

        try {
            GraficaIndicadorPromedioResponse response = indicadorPromedioService.obtenerGraficaIndicador(
                    negocio, puesto, indicador, anioDesde, usuario);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Parámetros inválidos",
                    "mensaje", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo gráfica de indicador: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error interno del servidor",
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/indicadores-promedio/actual
     * Obtiene comparación semana actual vs anterior para un indicador
     *
     * Ejemplo: GET /api/indicadores-promedio/actual?negocio=3&puesto=2319&indicador=1001&usuario=admin
     */
    @GetMapping("/actual")
    public ResponseEntity<?> obtenerIndicadorActual(
            @RequestParam Integer negocio,
            @RequestParam Integer puesto,
            @RequestParam Integer indicador,
            @RequestParam(defaultValue = "admin") String usuario) {

        logger.info("GET /api/indicadores-promedio/actual - Negocio={}, Puesto={}, Indicador={}",
                negocio, puesto, indicador);

        try {
            IndicadorPromedioResponse response = indicadorPromedioService.obtenerIndicadorPromedioActual(
                    negocio, puesto, indicador, usuario);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Parámetros inválidos",
                    "mensaje", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo indicador actual: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error interno del servidor",
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/indicadores-promedio/procesar
     * Procesa semanas faltantes para un indicador específico
     *
     * Ejemplo: POST /api/indicadores-promedio/procesar
     * Body: {
     *   "negocio": 3,
     *   "puesto": 2319,
     *   "indicador": 1001,
     *   "usuario": "admin"
     * }
     */
    @PostMapping("/procesar")
    public ResponseEntity<?> procesarSemanasFaltantes(@RequestBody Map<String, Object> request) {
        logger.info("POST /api/indicadores-promedio/procesar - Request: {}", request);

        try {
            Integer negocio = (Integer) request.get("negocio");
            Integer puesto = (Integer) request.get("puesto");
            Integer indicador = (Integer) request.get("indicador");
            String usuario = (String) request.getOrDefault("usuario", "admin");

            if (negocio == null || puesto == null || indicador == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Parámetros faltantes",
                        "mensaje", "Se requieren: negocio, puesto, indicador"
                ));
            }

            int semanasProcessadas = indicadorPromedioService.procesarSemanasFaltantesIndicador(
                    negocio, puesto, indicador, usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Procesamiento completado",
                    "semanasProcessadas", semanasProcessadas,
                    "negocio", negocio,
                    "puesto", puesto,
                    "indicador", indicador
            ));

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Parámetros inválidos",
                    "mensaje", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error procesando semanas faltantes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error interno del servidor",
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/indicadores-promedio/disponibles
     * Lista todas las combinaciones disponibles de indicadores
     *
     * Ejemplo: GET /api/indicadores-promedio/disponibles
     */
    @GetMapping("/disponibles")
    public ResponseEntity<?> listarIndicadoresDisponibles() {
        logger.info("GET /api/indicadores-promedio/disponibles");

        try {
            List<Map<String, Object>> combinaciones = indicadorPromedioService.listarIndicadoresDisponibles();

            return ResponseEntity.ok(Map.of(
                    "total", combinaciones.size(),
                    "combinaciones", combinaciones
            ));

        } catch (Exception e) {
            logger.error("Error listando indicadores disponibles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error interno del servidor",
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/indicadores-promedio/health
     * Health check del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "IndicadorPromedioService",
                "timestamp", System.currentTimeMillis()
        ));
    }
}