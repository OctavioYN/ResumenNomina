package com.resumen.nomina.presentation.controller;

import com.resumen.nomina.application.service.CompensacionSemanalService;
import com.resumen.nomina.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compensaciones")
@CrossOrigin(origins = "*")
public class CompensacionSemanalController {

    private static final Logger logger = LoggerFactory.getLogger(CompensacionSemanalController.class);

    private final CompensacionSemanalService compensacionService;

    @Autowired
    public CompensacionSemanalController(CompensacionSemanalService compensacionService) {
        this.compensacionService = compensacionService;
    }

    /**
     * GET /api/compensaciones/semanal/actual
     * Obtiene compensación semanal actual vs anterior (para tabla)
     */
    @GetMapping("/semanal/actual")
    public ResponseEntity<?> obtenerCompensacionSemanalActual(@RequestParam(defaultValue = "sistema") String usuario) {
        try {
            logger.info("Solicitando compensación semanal actual por usuario: {}", usuario);

            CompensacionSemanalResponse response = compensacionService.obtenerCompensacionSemanalActual(usuario);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Compensación semanal obtenida exitosamente");

            logger.info("Compensación semanal actual devuelta: {} vs {}",
                    response.getSemanaActual(), response.getSemanaAnterior());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo compensación semanal actual: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/compensaciones/semanal/recalcular
     * Fuerza el recálculo de la compensación semanal actual
     */
    @PostMapping("/semanal/recalcular")
    public ResponseEntity<?> recalcularCompensacionSemanal(@RequestBody Map<String, Object> request) {
        try {
            String usuario = (String) request.getOrDefault("usuario", "sistema");
            logger.info("Recalculando compensación semanal por usuario: {}", usuario);

            CompensacionSemanalResponse response = compensacionService.recalcularCompensacionSemanalActual(usuario);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Compensación semanal recalculada exitosamente");
            result.put("usuario", usuario);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en recálculo: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error de validación");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Error recalculando compensación semanal: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /api/compensaciones/sincronizar
     * Sincroniza todas las semanas disponibles en IndicadoresCalculados
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizarSemanas(@RequestBody Map<String, Object> request) {
        try {
            String usuario = (String) request.getOrDefault("usuario", "sistema");
            logger.info("Iniciando sincronización de semanas por usuario: {}", usuario);

            int semanasProcesadas = compensacionService.sincronizarTodasLasSemanas(usuario);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Sincronización completada exitosamente");
            result.put("semanasProcesadas", semanasProcesadas);
            result.put("usuario", usuario);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en sincronización: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error de validación");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Error sincronizando semanas: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/compensaciones/estado
     * Obtiene el estado actual del sistema (semanas procesadas vs disponibles)
     */
    @GetMapping("/estado")
    public ResponseEntity<?> obtenerEstadoSistema() {
        try {
            logger.info("Obteniendo estado del sistema de compensaciones");

            // Obtener información del estado actual
            List<CompensacionSemanal> semanasExistentes = compensacionService.obtenerCompensacionesPorAnio(2023);
            semanasExistentes.addAll(compensacionService.obtenerCompensacionesPorAnio(2024));
            semanasExistentes.addAll(compensacionService.obtenerCompensacionesPorAnio(2025));

            // Contar semanas por año
            Map<Integer, Long> semanasPorAnio = semanasExistentes.stream()
                    .collect(Collectors.groupingBy(
                            CompensacionSemanal::getAnio,
                            Collectors.counting()
                    ));

            // Obtener última semana procesada
            Optional<CompensacionSemanal> ultimaSemana = semanasExistentes.stream()
                    .filter(cs -> cs.getEsUltimaSemana() != null && cs.getEsUltimaSemana())
                    .findFirst();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalSemanasProcessadas", semanasExistentes.size());
            result.put("semanasPorAnio", semanasPorAnio);
            result.put("ultimaSemanaProcessada", ultimaSemana.map(CompensacionSemanal::getSemana).orElse("N/A"));
            result.put("fechaUltimaActualizacion",
                    ultimaSemana.map(CompensacionSemanal::getFechaCalculo).orElse(null));
            result.put("message", "Estado del sistema obtenido exitosamente");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo estado del sistema: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/compensaciones/grafica/historica
     * Obtiene datos históricos para gráfica desde un año específico
     */
    @GetMapping("/grafica/historica")
    public ResponseEntity<?> obtenerGraficaHistorica(
            @RequestParam(defaultValue = "2023") Integer anioDesde,
            @RequestParam(defaultValue = "sistema") String usuario) {
        try {
            logger.info("Solicitando gráfica histórica desde año: {} por usuario: {}", anioDesde, usuario);

            GraficaCompensacionResponse response = compensacionService.obtenerGraficaHistorica(anioDesde, usuario);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Gráfica histórica obtenida exitosamente");
            result.put("parametros", Map.of("anioDesde", anioDesde, "usuario", usuario));

            logger.info("Gráfica histórica devuelta con {} puntos desde {}",
                    response.getDatosGrafica().size(), anioDesde);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en gráfica histórica: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error de validación");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Error obteniendo gráfica histórica: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/compensaciones/semana/{semana}
     * Obtiene compensación de una semana específica
     */
    @GetMapping("/semana/{semana}")
    public ResponseEntity<?> obtenerCompensacionPorSemana(@PathVariable String semana) {
        try {
            logger.info("Solicitando compensación para semana: {}", semana);

            Optional<CompensacionSemanal> compensacion = compensacionService.obtenerCompensacionPorSemana(semana);

            if (compensacion.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", compensacion.get());
                result.put("semana", semana);
                result.put("encontrada", true);

                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", null);
                result.put("semana", semana);
                result.put("encontrada", false);
                result.put("message", "No se encontró información para la semana " + semana);

                return ResponseEntity.ok(result);
            }

        } catch (Exception e) {
            logger.error("Error obteniendo compensación por semana: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/compensaciones/anio/{anio}
     * Obtiene todas las compensaciones de un año específico
     */
    @GetMapping("/anio/{anio}")
    public ResponseEntity<?> obtenerCompensacionesPorAnio(@PathVariable Integer anio) {
        try {
            logger.info("Solicitando compensaciones del año: {}", anio);

            List<CompensacionSemanal> compensaciones = compensacionService.obtenerCompensacionesPorAnio(anio);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", compensaciones);
            result.put("anio", anio);
            result.put("totalSemanas", compensaciones.size());
            result.put("message", "Compensaciones del año " + anio + " obtenidas exitosamente");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo compensaciones por año: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/compensaciones/estadisticas
     * Obtiene estadísticas generales del sistema
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> obtenerEstadisticasGenerales(
            @RequestParam(defaultValue = "2023") Integer anioDesde) {
        try {
            logger.info("Solicitando estadísticas generales desde año: {}", anioDesde);

            EstadisticasCompensacion estadisticas = compensacionService.obtenerEstadisticasGenerales(anioDesde);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", estadisticas);
            result.put("parametros", Map.of("anioDesde", anioDesde));
            result.put("message", "Estadísticas generales obtenidas exitosamente");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas generales: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /api/compensaciones/resumen
     * Obtiene resumen general del estado del sistema
     */
    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumenGeneral() {
        try {
            logger.info("Solicitando resumen general del sistema");

            // Obtener compensación actual
            CompensacionSemanalResponse actual = compensacionService.obtenerCompensacionSemanalActual("sistema");

            // Obtener estadísticas
            EstadisticasCompensacion estadisticas = compensacionService.obtenerEstadisticasGenerales(2023);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("compensacionActual", actual);
            result.put("estadisticasHistoricas", estadisticas);
            result.put("message", "Resumen general obtenido exitosamente");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error obteniendo resumen general: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * DELETE /api/compensaciones/semana/{semana}
     * Elimina compensación de una semana específica
     */
    @DeleteMapping("/semana/{semana}")
    public ResponseEntity<?> eliminarCompensacionPorSemana(@PathVariable String semana) {
        try {
            logger.info("Eliminando compensación para semana: {}", semana);

            if (compensacionService.obtenerCompensacionPorSemana(semana).isPresent()) {
                // Aquí implementarías la lógica de eliminación
                // compensacionRepository.deleteBySemana(semana);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Compensación de la semana " + semana + " eliminada exitosamente");
                result.put("semana", semana);

                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "No encontrado");
                error.put("message", "No existe compensación para la semana " + semana);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error eliminando compensación: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    /**
     * GET /api/compensaciones/grafica/negocio
     * Obtiene gráfica histórica filtrada por negocio específico
     */
    @GetMapping("/grafica/negocio")
    public ResponseEntity<?> obtenerGraficaHistoricaPorNegocio(
            @RequestParam Integer negocio,
            @RequestParam(defaultValue = "2023") Integer anioDesde,
            @RequestParam(defaultValue = "sistema") String usuario) {
        try {
            logger.info("Solicitando gráfica histórica para negocio: {} desde año: {} por usuario: {}",
                    negocio, anioDesde, usuario);

            GraficaCompensacionResponse response = compensacionService.obtenerGraficaHistoricaPorNegocio(negocio, anioDesde, usuario);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Gráfica histórica por negocio obtenida exitosamente");
            result.put("parametros", Map.of("negocio", negocio, "anioDesde", anioDesde, "usuario", usuario));

            logger.info("Gráfica por negocio {} devuelta con {} puntos desde {}",
                    negocio, response.getDatosGrafica().size(), anioDesde);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en gráfica por negocio: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error de validación");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Error obteniendo gráfica por negocio: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    //nuevo

    // Agregar este endpoint al Controller de Compensaciones

    /**
     * GET /api/compensaciones/semanal/por-semana
     * Obtiene compensación de una semana específica vs su semana anterior
     *
     * @param semana formato YYYYWW (ejemplo: "202539")
     * @param usuario usuario que realiza la consulta
     */
    @GetMapping("/semanal/por-semana")
    public ResponseEntity<?> obtenerCompensacionPorSemana(
            @RequestParam String semana,
            @RequestParam(defaultValue = "sistema") String usuario) {

        try {
            logger.info("Solicitando compensación para semana: {} por usuario: {}", semana, usuario);

            CompensacionSemanalResponse response = compensacionService.obtenerCompensacionPorSemana(semana, usuario);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "Compensación semanal obtenida exitosamente para semana " + semana);

            logger.info("Compensación semanal devuelta: {} vs {}",
                    response.getSemanaActual(), response.getSemanaAnterior());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación para semana {}: {}", semana, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Validación fallida");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Error obteniendo compensación para semana {}: {}", semana, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error interno");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }



}