package com.resumen.nomina.admin.controller;

import com.resumen.nomina.admin.dto.ResultadoCarga;
import com.resumen.nomina.admin.service.AdminCatalogoService;
import com.resumen.nomina.admin.service.AdminDatosInteligenciaService;
import com.resumen.nomina.application.service.CalculoIndicadorService;
import com.resumen.nomina.application.service.CompensacionSemanalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 🎮 CONTROLADOR DE ADMINISTRACIÓN
 * Endpoints para gestión de datos, cargas masivas y operaciones administrativas
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDatosInteligenciaService datosService;
    private final AdminCatalogoService catalogoService;
    private final CalculoIndicadorService calculoService;
    private final CompensacionSemanalService compensacionService;
    private final MongoTemplate mongoTemplate;

    // ========================================
    // 📊 GESTIÓN DE DATOS INTELIGENCIA
    // ========================================

    /**
     * POST /api/admin/datos-inteligencia/cargar-completa
     * Carga COMPLETA: Elimina todo y carga nuevo archivo
     *
     * @param file Archivo Excel con formato esperado
     * @param usuario Usuario que realiza la operación
     */
    @PostMapping(value = "/datos-inteligencia/cargar-completa",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarDatosCompleta(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "admin") String usuario) {

        log.info("📥 POST /cargar-completa - Archivo: {}, Usuario: {}",
                file.getOriginalFilename(), usuario);

        try {
            // Validaciones
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "El archivo está vacío"
                ));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "Solo se aceptan archivos Excel (.xlsx, .xls)"
                ));
            }

            // Ejecutar carga completa
            ResultadoCarga resultado = datosService.cargarDatosReemplazar(file, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", resultado.isSuccess());
            response.put("mensaje", resultado.getMensaje());
            response.put("totalRegistros", resultado.getTotalRegistros());
            response.put("registrosExitosos", resultado.getRegistrosExitosos());
            response.put("tiempoMs", resultado.getTiempoProcesamientoMs());
            response.put("fechaCarga", resultado.getFechaCarga());

            if (!resultado.getErrores().isEmpty()) {
                response.put("errores", resultado.getErrores());
            }

            return resultado.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            log.error("❌ Error en carga completa: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", "Error procesando archivo: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/admin/datos-inteligencia/cargar-incremental
     * Carga INCREMENTAL: Solo agrega o actualiza registros
     */
    @PostMapping(value = "/datos-inteligencia/cargar-incremental",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarDatosIncremental(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "admin") String usuario) {

        log.info("📥 POST /cargar-incremental - Archivo: {}, Usuario: {}",
                file.getOriginalFilename(), usuario);

        try {
            if (file.isEmpty() || file.getOriginalFilename() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "Archivo inválido"
                ));
            }

            ResultadoCarga resultado = datosService.cargarDatosIncremental(file, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", resultado.isSuccess());
            response.put("mensaje", resultado.getMensaje());
            response.put("totalRegistros", resultado.getTotalRegistros());
            response.put("registrosExitosos", resultado.getRegistrosExitosos());
            response.put("registrosConError", resultado.getRegistrosConError());
            response.put("tiempoMs", resultado.getTiempoProcesamientoMs());

            if (!resultado.getErrores().isEmpty()) {
                response.put("errores", resultado.getErrores());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error en carga incremental: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/admin/datos-inteligencia/periodo/{periodo}
     * Elimina todos los registros de un período específico
     */
    @DeleteMapping("/datos-inteligencia/periodo/{periodo}")
    public ResponseEntity<Map<String, Object>> eliminarPorPeriodo(
            @PathVariable String periodo,
            @RequestParam(defaultValue = "admin") String usuario) {

        log.info("🗑️ DELETE /periodo/{} - Usuario: {}", periodo, usuario);

        try {
            ResultadoCarga resultado = datosService.eliminarPorPeriodo(periodo, usuario);

            return ResponseEntity.ok(Map.of(
                    "success", resultado.isSuccess(),
                    "mensaje", resultado.getMensaje(),
                    "registrosEliminados", resultado.getTotalRegistros()
            ));

        } catch (Exception e) {
            log.error("❌ Error eliminando período: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    // ========================================
    // 📚 GESTIÓN DE CATÁLOGO TEXTOS
    // ========================================

    /**
     * POST /api/admin/catalogo/cargar
     * Carga archivo Excel de catálogos
     *
     * @param reemplazar true = elimina todo antes, false = actualiza/inserta
     */
    @PostMapping(value = "/catalogo/cargar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> cargarCatalogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean reemplazar,
            @RequestParam(defaultValue = "admin") String usuario) {

        log.info("📥 POST /catalogo/cargar - Archivo: {}, Reemplazar: {}, Usuario: {}",
                file.getOriginalFilename(), reemplazar, usuario);

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "Archivo vacío"
                ));
            }

            ResultadoCarga resultado = catalogoService.cargarCatalogo(file, usuario, reemplazar);

            Map<String, Object> response = new HashMap<>();
            response.put("success", resultado.isSuccess());
            response.put("mensaje", resultado.getMensaje());
            response.put("totalRegistros", resultado.getTotalRegistros());
            response.put("registrosExitosos", resultado.getRegistrosExitosos());
            response.put("registrosConError", resultado.getRegistrosConError());
            response.put("tiempoMs", resultado.getTiempoProcesamientoMs());
            response.put("modoReemplazar", reemplazar);

            if (!resultado.getErrores().isEmpty()) {
                response.put("errores", resultado.getErrores());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error cargando catálogo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    // ========================================
    // 🔄 OPERACIONES ADMINISTRATIVAS
    // ========================================

    /**
     * POST /api/admin/recalcular-todo
     * Recalcula TODOS los indicadores y compensaciones
     */
    @PostMapping("/recalcular-todo")
    public ResponseEntity<Map<String, Object>> recalcularTodo(
            @RequestBody Map<String, Object> request) {

        log.info("🔄 POST /recalcular-todo - Request: {}", request);

        try {
            String periodoInicial = (String) request.get("periodoInicial");
            String periodoFinal = (String) request.get("periodoFinal");
            String usuario = (String) request.getOrDefault("usuario", "admin");

            // Validaciones
            if (periodoInicial == null || periodoInicial.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "periodoInicial es requerido"
                ));
            }

            // 1. Recalcular indicadores
            log.info("📊 Recalculando indicadores desde {} hasta {}", periodoInicial, periodoFinal);
            // Aquí debes llamar al método que ya tienes en CalculoIndicadorController
            // que recalcula por rango

            // 2. Sincronizar compensaciones
            log.info("💰 Sincronizando compensaciones semanales");
            int semanasProcessadas = compensacionService.sincronizarTodasLasSemanas(usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Recálculo completo exitoso");
            response.put("semanasCompensacionProcessadas", semanasProcessadas);
            response.put("periodoInicial", periodoInicial);
            response.put("periodoFinal", periodoFinal);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error en recálculo completo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/admin/limpiar-coleccion
     * Limpia una colección específica
     */
    @PostMapping("/limpiar-coleccion")
    public ResponseEntity<Map<String, Object>> limpiarColeccion(
            @RequestBody Map<String, Object> request) {

        String coleccion = (String) request.get("coleccion");
        String usuario = (String) request.getOrDefault("usuario", "admin");

        log.warn("⚠️ POST /limpiar-coleccion - Colección: {}, Usuario: {}", coleccion, usuario);

        try {
            if (coleccion == null || coleccion.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "Nombre de colección requerido"
                ));
            }

            // Validar que sea una colección permitida
            if (!coleccionesPermitidas().contains(coleccion)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "mensaje", "Colección no permitida para limpieza: " + coleccion
                ));
            }

            long count = mongoTemplate.getCollection(coleccion).countDocuments();
            mongoTemplate.dropCollection(coleccion);

            log.info("✅ Colección '{}' limpiada: {} registros eliminados", coleccion, count);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mensaje", "Colección '" + coleccion + "' limpiada exitosamente",
                    "registrosEliminados", count
            ));

        } catch (Exception e) {
            log.error("❌ Error limpiando colección: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/admin/estadisticas
     * Obtiene estadísticas generales del sistema
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.info("📊 GET /estadisticas");

        try {
            Map<String, Object> stats = new HashMap<>();

            // Contar registros en cada colección
            stats.put("DatosInteligencia",
                    mongoTemplate.getCollection("DatosInteligencia").countDocuments());
            stats.put("IndicadoresCalculados",
                    mongoTemplate.getCollection("IndicadoresCalculados").countDocuments());
            stats.put("CompensacionesSemana",
                    mongoTemplate.getCollection("CompensacionesSemana").countDocuments());
            stats.put("catalogoTextos",
                    mongoTemplate.getCollection("catalogoTextos").countDocuments());
            stats.put("ConfiguracionAlertas",
                    mongoTemplate.getCollection("ConfiguracionAlertas").countDocuments());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("estadisticas", stats);
            response.put("colecciones", mongoTemplate.getCollectionNames());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/admin/health
     * Health check del módulo de administración
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "servicio", "Módulo de Administración",
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ========== MÉTODOS AUXILIARES ==========

    private java.util.List<String> coleccionesPermitidas() {
        return java.util.Arrays.asList(
                "IndicadoresCalculados",
                "CompensacionesSemana",
                "IndicadoresPromedio"
        );
    }
}