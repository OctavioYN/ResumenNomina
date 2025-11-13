package com.resumen.nomina.presentation.controller;


import com.resumen.nomina.application.service.IndicadorReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/indicadores")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IndicadorReporteController {

    private final IndicadorReporteService service;

    /**
     * CAMBIO PRINCIPAL: Ahora solo recibe periodoActual como parámetro
     * Antes: @RequestParam(defaultValue = "202537,202536") String periodos
     * Ahora: @RequestParam(defaultValue = "202538") String periodoActual
     */
    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> obtenerTotales(
            @RequestParam(defaultValue = "202538") String periodoActual) {

        log.info("=== GET /total - período actual: {} ===", periodoActual);

        try {
            // CAMBIO: Llamada al service con un solo parámetro
            List<Document> resultados = service.obtenerTotales(periodoActual);

            // NUEVO: Extraer período anterior del primer resultado (todos tienen el mismo)
            String periodoAnterior = "N/A";
            if (!resultados.isEmpty()) {
                periodoAnterior = resultados.get(0).getString("periodoAnterior");
            }

            // CAMBIO: Respuesta incluye períodos por separado
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "TOTAL");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("periodoAnterior", periodoAnterior); // <- NUEVO CAMPO
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            log.info("Respondiendo con {} registros (Actual: {}, Anterior: {})",
                    resultados.size(), periodoActual, periodoAnterior);
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener totales para período {}: {}", periodoActual, e.getMessage(), e);

            // NUEVO: Manejo de errores más detallado
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("tipo", "TOTAL");
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());
            errorResponse.put("periodoActual", periodoActual);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * CAMBIO PRINCIPAL: Ahora solo recibe periodoActual como parámetro
     */
    @GetMapping("/promedio")
    public ResponseEntity<Map<String, Object>> obtenerPromedios(
            @RequestParam(defaultValue = "202538") String periodoActual) {

        log.info("=== GET /promedio - período actual: {} ===", periodoActual);

        try {
            // CAMBIO: Llamada al service con un solo parámetro
            List<Document> resultados = service.obtenerPromedios(periodoActual);

            // NUEVO: Extraer período anterior del primer resultado
            String periodoAnterior = "N/A";
            if (!resultados.isEmpty()) {
                periodoAnterior = resultados.get(0).getString("periodoAnterior");
            }

            // CAMBIO: Respuesta incluye períodos por separado
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "PROMEDIO");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("periodoAnterior", periodoAnterior); // <- NUEVO CAMPO
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            log.info("Respondiendo con {} registros (Actual: {}, Anterior: {})",
                    resultados.size(), periodoActual, periodoAnterior);
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener promedios para período {}: {}", periodoActual, e.getMessage(), e);

            // NUEVO: Manejo de errores más detallado
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("tipo", "PROMEDIO");
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());
            errorResponse.put("periodoActual", periodoActual);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * NUEVO ENDPOINT: Para verificar datos disponibles
     */
    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificarDatos(
            @RequestParam(defaultValue = "202538") String periodoActual) {

        log.info("=== GET /verificar - período actual: {} ===", periodoActual);

        try {
            Map<String, Object> verificacion = service.verificarDatos(periodoActual);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("verificacion", verificacion);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al verificar datos para período {}: {}", periodoActual, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al verificar datos: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/mapaCalor")
    public ResponseEntity<Map<String, Object>> obtenerMapaCalor(
            @RequestParam String periodoActual) {  // Cambié el valor por defecto, ahora recibe el parámetro directamente

        log.info("=== GET /mapaCalor - período actual: {} ===", periodoActual);

        try {
            // Validar el formato del período
            service.validarPeriodo(periodoActual);  // Asegúrate de que el servicio valide el período

            // Obtener los datos totales desde el servicio, que ya responde de acuerdo al período enviado
            List<Document> resultados = service.obtenerTotales(periodoActual);

            // Preparar la respuesta para el mapa de calor
            List<Map<String, Object>> mapaCalorData = new ArrayList<>();

            for (Document doc : resultados) {
                // Extraer datos del documento
                String puesto = doc.getString("fcDetalle5");
                String sucursal = doc.getString("sucursal");
                Double variacion = doc.getDouble("variacion");

                // Asignar color según la variación
                String color = asignarColor(variacion);

                // Construir el mapa de calor con puesto, variación y color
                Map<String, Object> mapaData = new HashMap<>();
                mapaData.put("negocio",sucursal);
                mapaData.put("puesto", puesto);
                mapaData.put("variacion", variacion);
                mapaData.put("color", color); // Color según la variación

                mapaCalorData.add(mapaData);
            }

            // Responder con los datos para el mapa de calor
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "MAPA_CALOR");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("total", mapaCalorData.size());
            respuesta.put("datos", mapaCalorData);

            log.info("Respondiendo con {} registros para el mapa de calor (Actual: {})",
                    mapaCalorData.size(), periodoActual);
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener datos para el mapa de calor para el período {}: {}", periodoActual, e.getMessage(), e);

            // Manejo de errores
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());
            errorResponse.put("periodoActual", periodoActual);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

   /* private String asignarColor(Double variacion) {
        if (variacion > 10) {
            return "#4CAF50";  // Verde fuerte
        } else if (variacion > 5) {
            return "#8BC34A";  // Verde suave
        } else if (variacion > 0) {
            return "#CDDC39";  // Amarillo suave
        } else if (variacion == 0) {
            return "#FFEB3B";  // Amarillo
        } else if (variacion > -5) {
            return "#FF9800";  // Naranja
        } else if (variacion > -10) {
            return "#FF5722";  // Rojo suave
        } else {
            return "#F44336";  // Rojo fuerte
        }*/

        private String asignarColor(Double variacion) {
            if (variacion > 10) {
                return "#4E7A1E";  // Verde intenso
            } else if (variacion > 5) {
                return "#5D8C3F";  // Verde medio
            } else if (variacion > 0) {
                return "#8C9E5A";  // Verde oliva claro
            } else if (variacion == 0) {
                return "#7F6A00";  // Amarillo mostaza oscuro
            } else if (variacion > -5) {
                return "#B95D0D";  // Naranja quemado
            } else if (variacion > -10) {
                return "#9B2B1A";  // Rojo ladrillo
            } else {
                return "#9B2B1A";  // Rojo ladrillo (repetido para variaciones más negativas)
            }
        }





}