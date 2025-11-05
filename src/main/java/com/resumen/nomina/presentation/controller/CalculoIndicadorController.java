
package com.resumen.nomina.presentation.controller;

import com.resumen.nomina.application.service.CalculoIndicadorService;
import com.resumen.nomina.domain.model.Indicador;
import com.resumen.nomina.domain.model.IndicadorCalculado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

import java.time.temporal.IsoFields;
import java.util.Arrays;

@RestController
@RequestMapping("/api/calculos")
@CrossOrigin(origins = "*")
public class CalculoIndicadorController {

    private static final Logger logger = LoggerFactory.getLogger(CalculoIndicadorController.class);

    private final CalculoIndicadorService calculoIndicadorService;

    @Autowired
    public CalculoIndicadorController(CalculoIndicadorService calculoIndicadorService) {
        this.calculoIndicadorService = calculoIndicadorService;
    }

    /**
     * POST /api/calculos/consulta - Ejecuta cálculo sin guardar
     */
    @PostMapping("/consulta")
    public ResponseEntity<?> ejecutarConsulta(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> periodos = (List<String>) request.get("periodos");

            logger.info("Ejecutando consulta de cálculo para periodos: {}", periodos);

            List<Indicador> resultados = calculoIndicadorService.ejecutarCalculoConsulta(periodos);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalResultados", resultados.size());
            response.put("periodos", periodos);
            response.put("datos", resultados);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en consulta: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error ejecutando consulta: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/calculos/ejecutar - Ejecuta cálculo y guarda resultados
     */
    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutarYGuardarCalculo(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> periodos = (List<String>)  request.get("periodos");
            String usuario = (String) request.get("usuario");

            logger.info("Ejecutando cálculo y guardado para periodos: {} por usuario: {}", periodos, usuario);

            List<IndicadorCalculado> resultados = calculoIndicadorService.ejecutarCalculoYGuardar(periodos, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cálculo ejecutado y guardado exitosamente");
            response.put("totalGuardados", resultados.size());
            response.put("periodos", periodos);
            response.put("usuario", usuario);
            response.put("datos", resultados);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en ejecución: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error ejecutando y guardando cálculo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/calculos/recalcular - Recalcula y reemplaza cálculos existentes
     */
    @PostMapping("/recalcular")
    public ResponseEntity<?> recalcularIndicadores(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> periodos = (List<String>) request.get("periodos");
            String usuario = (String) request.get("usuario");

            logger.info("Recalculando indicadores para periodos: {} por usuario: {}", periodos, usuario);

            List<IndicadorCalculado> resultados = calculoIndicadorService.recalcularIndicadores(periodos, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Indicadores recalculados y reemplazados exitosamente");
            response.put("totalRecalculados", resultados.size());
            response.put("periodos", periodos);
            response.put("usuario", usuario);
            response.put("datos", resultados);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en recálculo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error recalculando indicadores: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/calculos - Obtiene todos los cálculos guardados
     */
    @GetMapping
    public ResponseEntity<?> obtenerTodosLosCalculos() {
        try {
            logger.info("Obteniendo todos los cálculos guardados");

            List<IndicadorCalculado> calculos = calculoIndicadorService.obtenerTodosLosCalculos();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCalculos", calculos.size());
            response.put("datos", calculos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo cálculos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/calculos/periodo/{periodo} - Obtiene cálculos por periodo
     */
    @GetMapping("/periodo/{periodo}")
    public ResponseEntity<?> obtenerCalculosPorPeriodo(@PathVariable String periodo) {
        try {
            logger.info("Obteniendo cálculos para periodo: {}", periodo);

            List<IndicadorCalculado> calculos = calculoIndicadorService.obtenerCalculosPorPeriodo(periodo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("periodo", periodo);
            response.put("totalCalculos", calculos.size());
            response.put("datos", calculos);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación para periodo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo cálculos por periodo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/calculos/negocio/{negocio} - Obtiene cálculos por negocio
     */
    @GetMapping("/negocio/{negocio}")
    public ResponseEntity<?> obtenerCalculosPorNegocio(@PathVariable Integer negocio) {
        try {
            logger.info("Obteniendo cálculos para negocio: {}", negocio);

            List<IndicadorCalculado> calculos = calculoIndicadorService.obtenerCalculosPorNegocio(negocio);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("negocio", negocio);
            response.put("totalCalculos", calculos.size());
            response.put("datos", calculos);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación para negocio: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo cálculos por negocio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/calculos/estadisticas - Obtiene estadísticas de los cálculos
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            logger.info("Generando estadísticas de cálculos");

            Map<String, Object> estadisticas = calculoIndicadorService.obtenerEstadisticasCalculos();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(estadisticas);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/calculos/resumen - Obtiene resumen de cálculos recientes
     */
    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumenReciente() {
        try {
            logger.info("Generando resumen de cálculos recientes");

            Map<String, Object> resumen = calculoIndicadorService.obtenerResumenReciente();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(resumen);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo resumen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/calculos/limpiar/{dias} - Limpia cálculos antiguos
     */
    @DeleteMapping("/limpiar/{dias}")
    public ResponseEntity<?> limpiarCalculosAntiguos(@PathVariable int dias) {
        try {
            logger.info("Iniciando limpieza de cálculos anteriores a {} días", dias);

            calculoIndicadorService.limpiarCalculosAntiguos(dias);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cálculos antiguos eliminados exitosamente");
            response.put("diasAntiguedad", dias);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en limpieza: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error limpiando cálculos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }
//desde aqui.

// Agregar estos imports si no están presentes:
// import java.util.Arrays;
// import java.time.LocalDate;
// import java.time.temporal.IsoFields;

  /*  @PostMapping("/recalcular-rango")
    public ResponseEntity<Map<String, Object>> recalcularIndicadoresPorRango(@RequestBody Map<String, Object> request) {
        try {
            String periodoInicial = (String) request.get("periodoInicial");
            String periodoFinal = (String) request.get("periodoFinal"); // Opcional, si no se envía usa el actual
            String usuario = (String) request.get("usuario");

            // Validar período inicial
            if (periodoInicial == null || periodoInicial.trim().isEmpty()) {
                throw new IllegalArgumentException("El período inicial es requerido");
            }|

            // Si no se especifica período final, usar el actual
            if (periodoFinal == null || periodoFinal.trim().isEmpty()) {
                // Detectar si el período inicial es semana o mes para usar el formato correcto
                boolean esSemana = esPeriodoSemana(periodoInicial);
                periodoFinal = obtenerPeriodoActual(esSemana);
            }

            logger.info("Recalculando indicadores desde período: {} hasta: {} por usuario: {}",
                    periodoInicial, periodoFinal, usuario);

            // Generar lista de períodos
            List<String> periodos = generarListaPeriodos(periodoInicial, periodoFinal);

            if (periodos.isEmpty()) {
                throw new IllegalArgumentException("No se generaron períodos válidos para el rango especificado");
            }

            logger.info("Períodos generados: {}", periodos);

            // Recalcular indicadores período a período (cada uno vs el anterior)
            List<Object> resultados = new ArrayList<>();

            logger.info("Procesando {} períodos de forma consecutiva (período actual vs anterior)", periodos.size());

            // Procesar cada período con el anterior: 202537 vs 202536, 202536 vs 202535, etc.
            for (int i = periodos.size() - 1; i > 0; i--) {
                String periodoActual = periodos.get(i);    // Período más reciente
                String periodoAnterior = periodos.get(i - 1); // Período anterior

                List<String> parPeriodos = Arrays.asList(periodoAnterior, periodoActual);

                logger.info("Calculando variación: {} vs {} (anterior vs actual)", periodoAnterior, periodoActual);

                try {
                    List<Object> resultadoPar = Collections.singletonList(calculoIndicadorService.recalcularIndicadores(parPeriodos, usuario));
                    resultados.addAll(resultadoPar);

                    logger.info("Completado cálculo para par: {} vs {}", periodoAnterior, periodoActual);
                } catch (Exception e) {
                    logger.error("Error calculando par {} vs {}: {}", periodoAnterior, periodoActual, e.getMessage());
                    // Continuar con el siguiente par en caso de error
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Indicadores recalculados exitosamente para el rango de períodos");
            response.put("totalRecalculados", resultados.size());
            response.put("periodoInicial", periodoInicial);
            response.put("periodoFinal", periodoFinal);
            response.put("totalPeriodos", periodos.size());
            response.put("periodos", periodos);
            response.put("usuario", usuario);
            response.put("datos", resultados);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en recálculo por rango: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error recalculando indicadores por rango: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Genera una lista de períodos desde el inicial hasta el final
     * Maneja correctamente semanas y cambios de año
     */
    /*private List<String> generarListaPeriodos(String periodoInicial, String periodoFinal) {
        List<String> periodos = new ArrayList<>();

        try {
            // Validar formato de períodos
            if (!validarFormatoPeriodo(periodoInicial) || !validarFormatoPeriodo(periodoFinal)) {
                throw new IllegalArgumentException("Formato de período inválido. Use YYYYSS para semanas (ej: 202537) o YYYYMM para meses (ej: 202301)");
            }

            int anoInicial = Integer.parseInt(periodoInicial.substring(0, 4));
            int periodoNumInicial = Integer.parseInt(periodoInicial.substring(4, 6));
            int anoFinal = Integer.parseInt(periodoFinal.substring(0, 4));
            int periodoNumFinal = Integer.parseInt(periodoFinal.substring(4, 6));

            // Validar que el período inicial sea menor o igual al final
            if (anoInicial > anoFinal || (anoInicial == anoFinal && periodoNumInicial > periodoNumFinal)) {
                throw new IllegalArgumentException("El período inicial debe ser menor o igual al período final");
            }

            int anoActual = anoInicial;
            int periodoActual = periodoNumInicial;

            while (anoActual < anoFinal || (anoActual == anoFinal && periodoActual <= periodoNumFinal)) {
                String periodo = String.format("%04d%02d", anoActual, periodoActual);
                periodos.add(periodo);

                periodoActual++;

                // Detectar si trabajamos con semanas (rango 1-53) o meses (rango 1-12)
                boolean esSemana = periodoNumInicial <= 53 && periodoNumFinal <= 53;
                int maxPeriodo = esSemana ? obtenerUltimaSemanaDelAno(anoActual) : 12;

                if (periodoActual > maxPeriodo) {
                    periodoActual = 1;
                    anoActual++;
                }
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al parsear los períodos: " + e.getMessage());
        }

        return periodos;
    }

    /**
     * Detecta si un período es semana (YYYYSS) o mes (YYYYMM)
     */
   /* private boolean esPeriodoSemana(String periodo) {
        if (periodo == null || periodo.length() != 6) {
            return false;
        }

        try {
            int num = Integer.parseInt(periodo.substring(4, 6));
            // Si el número es mayor a 12, probablemente es semana
            // Si es menor o igual a 12, podría ser mes o semana, pero asumimos mes
            return num > 12 && num <= 53;
        } catch (NumberFormatException e) {
            return false;
        }
    }*/

    /**
     * Obtiene la última semana del año (52 o 53 dependiendo del año)
     */
   /* private int obtenerUltimaSemanaDelAno(int ano) {
        // Calcular si el año tiene 53 semanas
        // Un año tiene 53 semanas si:
        // - Comienza en jueves (año normal) o
        // - Comienza en miércoles (año bisiesto)

        LocalDate primeroDiciembre31 = LocalDate.of(ano, 12, 31);
        LocalDate primerEnero1 = LocalDate.of(ano, 1, 1);

        // Obtener el día de la semana del 1 de enero (1=lunes, 7=domingo)
        int diaSemanaEnero1 = primerEnero1.getDayOfWeek().getValue();

        // Obtener el día de la semana del 31 de diciembre
        int diaSemanaDic31 = primeroDiciembre31.getDayOfWeek().getValue();

        // Si el año comienza en jueves (4) o el 31 de diciembre es jueves, tiene 53 semanas
        // Si es año bisiesto y comienza en miércoles (3), también tiene 53 semanas
        boolean esAnioBisiesto = LocalDate.of(ano, 1, 1).isLeapYear();

        if (diaSemanaEnero1 == 4 || diaSemanaDic31 == 4 || (esAnioBisiesto && diaSemanaEnero1 == 3)) {
            return 53;
        }

        return 52;
    }*/

    /**
     * Valida que el período tenga el formato correcto YYYYSS (año + semana) o YYYYMM (año + mes)
     */
    /*private boolean validarFormatoPeriodo(String periodo) {
        if (periodo == null || periodo.length() != 6) {
            return false;
        }

        try {
            int ano = Integer.parseInt(periodo.substring(0, 4));
            int num = Integer.parseInt(periodo.substring(4, 6));

            // Validar año razonable
            if (ano < 2020 || ano > 2030) {
                return false;
            }

            // Validar si es semana (1-53) o mes (1-12)
            if (num >= 1 && num <= 12) {
                return true; // Puede ser mes o semana
            } else if (num >= 13 && num <= 53) {
                return true; // Definitivamente es semana
            }

            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }*/

    /**
     * Obtiene el período actual en formato YYYYSS (semana) o YYYYMM (mes)
     * Por defecto devuelve semana, pero puedes cambiar el parámetro
     */
    private String obtenerPeriodoActual() {
        return obtenerPeriodoActual(true); // true = semana, false = mes
    }

    /*private String obtenerPeriodoActual(boolean esSemana) {
        LocalDate now = LocalDate.now();

        if (esSemana) {
            // Calcular número de semana del año (ISO 8601)
            int numeroSemana = now.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int anoSemana = now.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
            return String.format("%04d%02d", anoSemana, numeroSemana);
        } else {
            // Mes
            return String.format("%04d%02d", now.getYear(), now.getMonthValue());
        }
    }*/

    //aqui 2
    /**
     * ENDPOINTS MEJORADOS PARA TU CALCULOINDICADORCONTROLLER
     * Reemplaza los métodos existentes con estos optimizados
     */

    @PostMapping("/recalcular-hasta-actual")
    public ResponseEntity<Map<String, Object>> recalcularIndicadoresHastaActual(@RequestBody Map<String, Object> request) {
        try {
            String periodoInicial = (String) request.get("periodoInicial");
            String periodoFinal = (String) request.get("periodoFinal");
            String usuario = (String) request.get("usuario");

            // Validar período inicial
            if (periodoInicial == null || periodoInicial.trim().isEmpty()) {
                throw new IllegalArgumentException("El período inicial es requerido");
            }

            // Si no se especifica período final, usar el actual
            if (periodoFinal == null || periodoFinal.trim().isEmpty()) {
                periodoFinal = obtenerPeriodoActual(esPeriodoSemana(periodoInicial));
            }

            logger.info("Recalculando indicadores desde período: {} hasta: {} por usuario: {}",
                    periodoInicial, periodoFinal, usuario);

            // Generar lista de períodos
            List<String> periodos = generarListaPeriodos(periodoInicial, periodoFinal);

            if (periodos.isEmpty()) {
                throw new IllegalArgumentException("No se generaron períodos válidos");
            }

            logger.info("Procesando {} períodos: {}", periodos.size(), periodos);

            // Ejecutar cálculos período a período
            List<IndicadorCalculado> resultados = ejecutarCalculosPorPares(periodos, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Indicadores recalculados desde " + periodoInicial + " hasta " + periodoFinal);
            response.put("totalRecalculados", resultados.size());
            response.put("periodoInicial", periodoInicial);
            response.put("periodoFinal", periodoFinal);
            response.put("totalPeriodos", periodos.size());
            response.put("periodos", periodos);
            response.put("usuario", usuario);
            response.put("datos", resultados);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en recálculo hasta actual: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error recalculando indicadores hasta actual: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/recalcular-rango")
    public ResponseEntity<Map<String, Object>> recalcularIndicadoresPorRango(@RequestBody Map<String, Object> request) {
        try {
            String periodoInicial = (String) request.get("periodoInicial");
            String periodoFinal = (String) request.get("periodoFinal");
            String usuario = (String) request.get("usuario");

            // Validaciones
            if (periodoInicial == null || periodoInicial.trim().isEmpty()) {
                throw new IllegalArgumentException("El período inicial es requerido");
            }

            if (periodoFinal == null || periodoFinal.trim().isEmpty()) {
                periodoFinal = obtenerPeriodoActual(esPeriodoSemana(periodoInicial));
            }

            logger.info("Recalculando indicadores desde período: {} hasta: {} por usuario: {}",
                    periodoInicial, periodoFinal, usuario);

            // Generar lista de períodos
            List<String> periodos = generarListaPeriodos(periodoInicial, periodoFinal);

            if (periodos.isEmpty()) {
                throw new IllegalArgumentException("No se generaron períodos válidos para el rango especificado");
            }

            logger.info("Períodos generados: {}", periodos);

            // Ejecutar cálculos
            List<IndicadorCalculado> resultados = ejecutarCalculosPorPares(periodos, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Indicadores recalculados exitosamente para el rango de períodos");
            response.put("totalRecalculados", resultados.size());
            response.put("periodoInicial", periodoInicial);
            response.put("periodoFinal", periodoFinal);
            response.put("totalPeriodos", periodos.size());
            response.put("periodos", periodos);
            response.put("usuario", usuario);
            response.put("datos", resultados);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación en recálculo por rango: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error de validación",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error recalculando indicadores por rango: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error interno",
                    "message", e.getMessage()
            ));
        }
    }

// =====================================================
// MÉTODOS AUXILIARES OPTIMIZADOS
// =====================================================

    /**
     * Ejecuta cálculos para pares de períodos consecutivos
     * Optimizado para tu lógica de negocio
     */
    private List<IndicadorCalculado> ejecutarCalculosPorPares(List<String> periodos, String usuario) {
        List<IndicadorCalculado> todosLosResultados = new ArrayList<>();
        int paresEjecutados = 0;
        int errores = 0;

        logger.info("Iniciando cálculos para {} períodos (generará {} pares)",
                periodos.size(), periodos.size() - 1);

        // Procesar cada período con el anterior: del más reciente al más antiguo
        for (int i = periodos.size() - 1; i > 0; i--) {
            String periodoActual = periodos.get(i);      // Período más reciente
            String periodoAnterior = periodos.get(i - 1); // Período anterior

            List<String> parPeriodos = Arrays.asList(periodoAnterior, periodoActual);

            logger.info("Procesando par {}/{}: {} vs {} (anterior vs actual)",
                    paresEjecutados + 1, periodos.size() - 1, periodoAnterior, periodoActual);

            try {
                // Usar tu servicio existente que requiere exactamente 2 períodos
                List<IndicadorCalculado> resultadoPar = calculoIndicadorService.recalcularIndicadores(parPeriodos, usuario);

                if (resultadoPar != null && !resultadoPar.isEmpty()) {
                    todosLosResultados.addAll(resultadoPar);
                    paresEjecutados++;
                    logger.info("✅ Par completado: {} registros calculados", resultadoPar.size());
                } else {
                    logger.warn("⚠️ Sin resultados para el par: {} vs {}", periodoAnterior, periodoActual);
                }

            } catch (Exception e) {
                errores++;
                logger.error("❌ Error calculando par {} vs {}: {}", periodoAnterior, periodoActual, e.getMessage());

                // Decidir si continuar o abortar según tu lógica de negocio
                if (errores > 3) {
                    logger.error("Demasiados errores ({}), abortando proceso", errores);
                    throw new RuntimeException("Proceso abortado por exceso de errores: " + e.getMessage());
                }
            }
        }

        logger.info("🎉 Proceso completado: {} pares ejecutados, {} resultados totales, {} errores",
                paresEjecutados, todosLosResultados.size(), errores);

        return todosLosResultados;
    }

    /**
     * Genera períodos con manejo correcto de semanas/meses y cambios de año
     */
    private List<String> generarListaPeriodos(String periodoInicial, String periodoFinal) {
        List<String> periodos = new ArrayList<>();

        try {
            // Validar formato
            if (!validarFormatoPeriodo(periodoInicial) || !validarFormatoPeriodo(periodoFinal)) {
                throw new IllegalArgumentException("Formato de período inválido. Use YYYYSS para semanas o YYYYMM para meses");
            }

            int anoInicial = Integer.parseInt(periodoInicial.substring(0, 4));
            int numInicial = Integer.parseInt(periodoInicial.substring(4, 6));
            int anoFinal = Integer.parseInt(periodoFinal.substring(0, 4));
            int numFinal = Integer.parseInt(periodoFinal.substring(4, 6));

            // Validar orden lógico
            if (anoInicial > anoFinal || (anoInicial == anoFinal && numInicial > numFinal)) {
                throw new IllegalArgumentException("El período inicial debe ser menor o igual al período final");
            }

            // Detectar tipo (semana o mes)
            boolean esSemana = esPeriodoSemana(periodoInicial) || esPeriodoSemana(periodoFinal);

            int anoActual = anoInicial;
            int numActual = numInicial;

            while (anoActual < anoFinal || (anoActual == anoFinal && numActual <= numFinal)) {
                periodos.add(String.format("%04d%02d", anoActual, numActual));

                numActual++;

                // Manejar cambio de año
                int limite = esSemana ? obtenerUltimaSemanaDelAno(anoActual) : 12;
                if (numActual > limite) {
                    numActual = 1;
                    anoActual++;
                }
            }

            logger.info("Generados {} períodos desde {} hasta {}", periodos.size(), periodoInicial, periodoFinal);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al parsear los períodos: " + e.getMessage());
        }

        return periodos;
    }

    /**
     * Detecta si un período representa semanas (>12) o meses (≤12)
     */
    private boolean esPeriodoSemana(String periodo) {
        if (periodo == null || periodo.length() != 6) return false;

        try {
            int num = Integer.parseInt(periodo.substring(4, 6));
            return num > 12 && num <= 53;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Calcula las semanas del año (52 o 53 según ISO 8601)
     */
    private int obtenerUltimaSemanaDelAno(int ano) {
        LocalDate primerEnero = LocalDate.of(ano, 1, 1);
        LocalDate ultimoDiciembre = LocalDate.of(ano, 12, 31);

        int diaSemanaEnero = primerEnero.getDayOfWeek().getValue();
        int diaSemanaDiciembre = ultimoDiciembre.getDayOfWeek().getValue();
        boolean esBisiesto = primerEnero.isLeapYear();

        // ISO 8601: Año tiene 53 semanas si comienza en jueves o si es bisiesto y comienza en miércoles
        return (diaSemanaEnero == 4 || diaSemanaDiciembre == 4 || (esBisiesto && diaSemanaEnero == 3)) ? 53 : 52;
    }

    /**
     * Validador robusto de períodos
     */
    private boolean validarFormatoPeriodo(String periodo) {
        if (periodo == null || periodo.length() != 6) return false;

        try {
            int ano = Integer.parseInt(periodo.substring(0, 4));
            int num = Integer.parseInt(periodo.substring(4, 6));

            return ano >= 2020 && ano <= 2070 && num >= 1 &&
                    ((num <= 12) || (num <= 53)); // Mes o semana
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtiene período actual con formato automático
     */
    private String obtenerPeriodoActual(boolean esSemana) {
        LocalDate now = LocalDate.now();

        if (esSemana) {
            int semana = now.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int ano = now.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
            return String.format("%04d%02d", ano, semana);
        } else {
            return String.format("%04d%02d", now.getYear(), now.getMonthValue());
        }
    }
}