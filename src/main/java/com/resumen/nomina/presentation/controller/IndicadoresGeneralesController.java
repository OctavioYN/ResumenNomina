package com.resumen.nomina.presentation.controller;


import com.resumen.nomina.application.service.IndicadoresGeneralesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/indicadores-generales")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IndicadoresGeneralesController {

    private final IndicadoresGeneralesService service;

    /**
     * Obtener todos los indicadores por período actual
     */
    @GetMapping("/todos")
    public ResponseEntity<Map<String, Object>> obtenerTodosLosIndicadores(
            @RequestParam(defaultValue = "202538") String periodoActual) {

        log.info("=== GET /todos - período actual: {} ===", periodoActual);

        try {
            List<Document> resultados = service.obtenerTodosLosIndicadores(periodoActual);

            String periodoAnterior = "N/A";
            if (!resultados.isEmpty()) {
                periodoAnterior = resultados.get(0).getString("periodoAnterior");
            }

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "INDICADORES_GENERALES");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("periodoAnterior", periodoAnterior);
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            log.info("Respondiendo con {} indicadores (Actual: {}, Anterior: {})",
                    resultados.size(), periodoActual, periodoAnterior);
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener indicadores para período {}: {}", periodoActual, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("tipo", "INDICADORES_GENERALES");
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());
            errorResponse.put("periodoActual", periodoActual);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Filtrar por sucursal específica
     */
    @GetMapping("/por-sucursal")
    public ResponseEntity<Map<String, Object>> obtenerPorSucursal(
            @RequestParam(defaultValue = "202538") String periodoActual,
            @RequestParam String sucursal) {

        log.info("=== GET /por-sucursal - período: {}, sucursal: {} ===", periodoActual, sucursal);

        try {
            List<Document> resultados = service.obtenerIndicadoresPorSucursal(periodoActual, sucursal);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "INDICADORES_POR_SUCURSAL");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("sucursal", sucursal);
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener indicadores por sucursal: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Filtrar por rango de variación
     */
    @GetMapping("/por-variacion")
    public ResponseEntity<Map<String, Object>> obtenerPorRangoVariacion(
            @RequestParam(defaultValue = "202538") String periodoActual,
            @RequestParam(required = false) Double variacionMinima,
            @RequestParam(required = false) Double variacionMaxima) {

        log.info("=== GET /por-variacion - período: {}, min: {}, max: {} ===",
                periodoActual, variacionMinima, variacionMaxima);

        try {
            List<Document> resultados = service.obtenerIndicadoresPorVariacion(
                    periodoActual, variacionMinima, variacionMaxima);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "INDICADORES_POR_VARIACION");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("variacionMinima", variacionMinima);
            respuesta.put("variacionMaxima", variacionMaxima);
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener indicadores por variación: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


    /**
     * Filtrar por sucursal específica
     */
    /*@GetMapping("/por-sucursalPuesto")
    public ResponseEntity<Map<String, Object>> obtenerPorSucursalPuesto(
            @RequestParam(defaultValue = "202538") String periodoActual,
            //@RequestParam String sucursal,@RequestParam String puesto)
            @RequestParam Integer negocio,@RequestParam Integer puesto)
    {

        log.info("=== GET /por-sucursalPuesto - período: {}, sucursal: {},puesto: {} ===", periodoActual, negocio,puesto);

        try {
            List<Document> resultados = service.obtenerIndicadoresPorSucursalPuesto(periodoActual, negocio,puesto);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "INDICADORES_POR_SUCURSAL_PUESTO");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("sucursal", negocio);
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener indicadores por sucursal: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }*/

    @GetMapping("/por-sucursalPuesto")
    public ResponseEntity<Map<String, Object>> obtenerPorSucursalPuesto(
            @RequestParam(defaultValue = "202538") String periodoActual,
            @RequestParam Integer negocio,
            @RequestParam Integer puesto) {



        try {
            List<Document> resultados = service.obtenerIndicadoresPorSucursalPuesto(
                    periodoActual, negocio, puesto);

            log.info("Ejecutando por-sucursal");

            log.info("=== GET /por-sucursalPuesto - período: {}, negocio: {}, puesto: {} ===",
                    periodoActual, negocio, puesto);

            // Agregar tipo de dato basado en conceptoDetalle y puesto
           /* for (Document doc : resultados) {
                Integer conceptoDetalle = doc.getInteger("conceptoDetalle");
                Integer puestoDoc = doc.getInteger("puesto");

                log.info("=== GET /valores a buscar - conceptoDetalle: {}, negocio: {}, puesto: {} ===",
                        conceptoDetalle, negocio, puestoDoc);

                String tipoDato = determinarTipoDato(conceptoDetalle, puestoDoc);
                doc.append("tipoDato", tipoDato);
                doc.append("formatoValor", obtenerFormatoDescripcion(tipoDato));

                log.info("=== Tipo de dato a regresar -  tipoDato: {} ===",
                        tipoDato);
            }*/

            for (Document doc : resultados) {
                Integer conceptoDetalle = doc.getInteger("conceptoDetalle");
                Integer puestoDoc = doc.getInteger("puesto");

                log.info("=== GET /valores a buscar - conceptoDetalle: {}, negocio: {}, puesto: {} ===",
                        conceptoDetalle, negocio, puestoDoc);

                // Determina el tipo de dato
                String tipoDato = determinarTipoDato(conceptoDetalle, puestoDoc);
                doc.append("tipoDato", tipoDato);
                doc.append("formatoValor", obtenerFormatoDescripcion(tipoDato));

                log.info("=== Tipo de dato a regresar - tipoDato: {} ===", tipoDato);

                // Formatear valores según el tipo de dato calculado
                if (tipoDato.equals("1")) { // NUMERO
                    formatearValores(doc, "");  // Si es numérico, puedes agregar el símbolo '$' si lo deseas
                } else if (tipoDato.equals("2")) { // PORCENTAJE
                    formatearValores(doc, "%");  // Si es porcentaje, agregar el símbolo '%'
                } else if (tipoDato.equals("3")) { // DECIMAL
                    formatearValores(doc, "");  // No agregar símbolos, solo formato numérico
                } else { // MONEDA (por defecto)
                    formatearValores(doc, "$");
                }

                // Aseguramos que la variación porcentual siempre tenga el símbolo '%'
                formatVariacionPorcentual(doc);
            }


            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("tipo", "INDICADORES_POR_SUCURSAL_PUESTO");
            respuesta.put("periodoActual", periodoActual);
            respuesta.put("sucursal", negocio);
            respuesta.put("total", resultados.size());
            respuesta.put("datos", resultados);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error al obtener indicadores: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error al procesar la solicitud: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


// En tu controller:

    private String determinarTipoDato(Integer conceptoDetalle, Integer puesto) {
        // Log para debug
       log.info("Evaluando tipo para conceptoDetalle: {} y puesto: {}", conceptoDetalle, puesto);

        // Validación inicial
        if (conceptoDetalle == null || puesto == null) {
           log.info("Valores null detectados, retornando 0 (MONEDA)");
            return "0";
        }

        // CASO 1: NUMERICO - Crear listas para comparación más clara
        List<Integer> conceptosNumerico1 = Arrays.asList(1002, 1003);
        List<Integer> puestosNumerico1 = Arrays.asList(2306,2772,2767);

        if (conceptosNumerico1.contains(conceptoDetalle) && puestosNumerico1.contains(puesto)) {
           log.info("Match encontrado: NUMERICO (caso 1)");
            return "1";
        }

        List<Integer> conceptosNumerico2 = Arrays.asList(1002, 1005);
        List<Integer> puestosNumerico2 = Arrays.asList(2647, 2377, 2404, 2396, 2765);


        if (conceptosNumerico2.contains(conceptoDetalle) && puestosNumerico2.contains(puesto)) {
           log.info("Match encontrado: NUMERICO (caso 2)");
            return "1";
        }

        if (conceptoDetalle == 1002 && puesto == 2402) {
           log.info("Match encontrado: NUMERICO (caso 3)");
            return "1";
        }

        if (conceptoDetalle == 1004 && puesto == 2306) {
           log.info("Match encontrado: NUMERICO (caso 4)");
            return "1";
        }

        List<Integer> conceptosNumerico5 = Arrays.asList(1002, 1003, 1004, 1008, 1009, 1010);
        if (conceptosNumerico5.contains(conceptoDetalle) && puesto == 2777) {
           log.info("Match encontrado: NUMERICO (caso 5)");
            return "1";
        }

        if (conceptoDetalle == 1011) {
            log.info("Match encontrado: NUMERICO (caso 6)");
            return "1";
        }


        // CASO 2: PORCENTAJE
        if (conceptoDetalle == 1003 && puesto == 2313) {
           log.info("Match encontrado: PORCENTAJE (caso 1)");
            return "2";
        }

        List<Integer> puestosPorcentaje = Arrays.asList(2325, 2397);
        if (conceptoDetalle == 1005 && puestosPorcentaje.contains(puesto)) {
           log.info("Match encontrado: PORCENTAJE (caso 2)");
            return "2";
        }

        // CASO 3: DECIMAL (clientes aforados)
        List<Integer> puestosDecimal = Arrays.asList(2331, 2332, 2333);
        if (conceptoDetalle == 1007 && puestosDecimal.contains(puesto)) {
           log.info("Match encontrado: DECIMAL");
            return "3";
        }

        // Default: MONEDA
       log.info("No se encontró match, retornando 0 (MONEDA)");
        return "0";
    }

    private String obtenerFormatoDescripcion(String tipo) {
        switch (tipo) {
            case "0": return "MONEDA";
            case "1": return "NUMERO";
            case "2": return "PORCENTAJE";
            case "3": return "DECIMAL";
            default: return "MONEDA";
        }
    }



    private void formatearValores(Document doc, String simbolo) {
        // Formatear el valor según el tipo
        formatValor(doc, "valorActual", simbolo);
        formatValor(doc, "valorAnterior", simbolo);
        formatValor(doc, "diferenciaPesos", simbolo);
    }

   /* private void formatValor(Document doc, String campo, String simbolo) {
        // Obtener el valor y formatearlo según el tipo de dato
        Object valor = doc.get(campo);
        if (valor != null) {
            if (valor instanceof Number) {
                // Formato numérico con 2 decimales
                double valorDouble = ((Number) valor).doubleValue();
                String valorFormateado = String.format("%.2f", valorDouble);
                // Si el tipo es MONEDA o NUMERO, agregamos el símbolo
                doc.append(campo, simbolo + valorFormateado);
            } else {
                doc.append(campo, "0"); // En caso de que el valor sea nulo o no numérico
            }
        }
    }*/

    /*private void formatValor(Document doc, String campo, String simbolo) {
        // Obtener el valor y formatearlo según el tipo de dato
        Object valor = doc.get(campo);
        if (valor != null) {
            if (valor instanceof Number) {
                // Formateo numérico con 2 decimales y separadores de miles
                double valorDouble = ((Number) valor).doubleValue();
                String valorFormateado = String.format("%,.2f", valorDouble); // Formato con comas para miles
                // Si el tipo es MONEDA o NUMERO, agregamos el símbolo
                doc.append(campo, simbolo + valorFormateado);
            } else {
                doc.append(campo, "0"); // En caso de que el valor sea nulo o no numérico
            }
        }
    }*/

   /* private void formatValor(Document doc, String campo, String simbolo) {
        // Obtener el valor y formatearlo según el tipo de dato
        Object valor = doc.get(campo);
        if (valor != null) {
            if (valor instanceof Number) {
                double valorDouble = ((Number) valor).doubleValue();
                String valorFormateado;

                // Formato con separadores de miles y 2 decimales
                if (simbolo.equals("%")) {
                    // Si es porcentaje, el símbolo '%' va al final
                    valorFormateado = String.format("%,.2f", valorDouble) + simbolo;
                } else {
                    // Si es MONEDA o NUMERO, agregamos el símbolo al inicio
                    valorFormateado = simbolo + String.format("%,.2f", valorDouble);
                }

                doc.append(campo, valorFormateado);
            } else {
                doc.append(campo, "0"); // En caso de que el valor sea nulo o no numérico
            }
        }
    }*/

    private void formatValor(Document doc, String campo, String simbolo) {
        // Obtener el valor y formatearlo según el tipo de dato
        Object valor = doc.get(campo);
        if (valor != null) {
            if (valor instanceof Number) {
                double valorDouble = ((Number) valor).doubleValue();
                String valorFormateado;

                // Formato con separadores de miles y 2 decimales
                if (simbolo.equals("%")) {
                    // Si es porcentaje, el símbolo '%' va al final
                    valorFormateado = String.format("%,.2f", valorDouble) + simbolo;
                } else {
                    // Si es MONEDA o NUMERO, agregamos el símbolo al inicio
                    // Si el valor es negativo, el signo '-' irá antes del símbolo de moneda
                    valorFormateado = String.format("%,.2f", valorDouble);
                    if (valorDouble < 0) {
                        valorFormateado = "-" + simbolo + valorFormateado.substring(1); // Mantiene el '-' al principio
                    } else {
                        valorFormateado = simbolo + valorFormateado;
                    }
                }

                doc.append(campo, valorFormateado);
            } else {
                doc.append(campo, "0"); // En caso de que el valor sea nulo o no numérico
            }
        }
    }


    // Método específico para asegurarse de que la variación porcentual siempre tiene el '%'
    private void formatVariacionPorcentual(Document doc) {
        Object valor = doc.get("variacionPorcentual");
        if (valor != null) {
            if (valor instanceof Number) {
                // Formateamos la variación porcentual con el símbolo '%'
                double valorDouble = ((Number) valor).doubleValue();
                String valorFormateado = String.format("%.2f", valorDouble);
                doc.append("variacionPorcentual", valorFormateado + "%");
            } else {
                doc.append("variacionPorcentual", "0%");
            }
        }
    }
}