package com.resumen.nomina.application.service;


import com.resumen.nomina.infrastructure.repository.IndicadorCalculadoRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicadorReporteService {

    private final IndicadorCalculadoRepositoryService repository;

    /**
     * CAMBIO PRINCIPAL: Ahora solo requiere periodoActual
     * Obtiene los totales de compensación para el período actual
     */
    public List<Document> obtenerTotales(String periodoActual) {
        log.info("=== SERVICE: Totales para período actual {} ===", periodoActual);

        // NUEVO: Validar parámetro
        validarPeriodo(periodoActual);

        // CAMBIO: Solo pasa periodoActual al repository
        List<Document> resultados = repository.obtenerIndicadoresCompletos(periodoActual);
        log.info("Resultados del repository: {}", resultados.size());

        // Procesar para vista TOTAL (sin cambios en la lógica de procesamiento)
        List<Document> totales = new ArrayList<>();
        for (Document doc : resultados) {
            Document total = new Document()
                    .append("puesto", doc.get("puesto"))
                    .append("sucursal", doc.get("sucursal"))
                    .append("fcDetalle5", doc.get("fcDetalle5"))
                    .append("negocio", doc.get("negocio"))
                    .append("periodoAnterior", doc.get("periodoAnterior"))
                    .append("periodoActual", doc.get("periodoActual"))
                    // VALORES DE TOTAL (valorActual y valorAnterior son totales de compensación)
                    .append("valorAnterior", doc.get("valorAnterior"))
                    .append("valorActual", doc.get("valorActual"))
                    .append("diferencia", doc.get("diferencia"))
                    .append("variacion", doc.get("variacion"))
                    // Información adicional
                    .append("empleadosActual", doc.get("empleadosActual"))
                    .append("empleadosAnterior", doc.get("empleadosAnterior"));

            totales.add(total);
        }

        log.info("Totales procesados: {}", totales.size());
        return totales;
    }

    /**
     * CAMBIO PRINCIPAL: Ahora solo requiere periodoActual
     * Obtiene los promedios de compensación para el período actual
     */
    public List<Document> obtenerPromedios(String periodoActual) {
        log.info("=== SERVICE: Promedios para período actual {} ===", periodoActual);

        // NUEVO: Validar parámetro
        validarPeriodo(periodoActual);

        // CAMBIO: Solo pasa periodoActual al repository
        List<Document> resultados = repository.obtenerIndicadoresCompletos(periodoActual);
        log.info("Resultados del repository: {}", resultados.size());

        // Procesar para vista PROMEDIO
        List<Document> promedios = new ArrayList<>();
        for (Document doc : resultados) {
            Document promedio = new Document()
                    .append("puesto", doc.get("puesto"))
                    .append("sucursal", doc.get("sucursal"))
                    .append("fcDetalle5", doc.get("fcDetalle5"))
                    .append("negocio", doc.get("negocio"))
                    .append("periodoAnterior", doc.get("periodoAnterior"))
                    .append("periodoActual", doc.get("periodoActual"))
                    // VALORES DE PROMEDIO (promedioActual y promedioAnterior)
                    .append("valorAnterior", doc.get("promedioAnterior"))
                    .append("valorActual", doc.get("promedioActual"))
                    .append("diferencia", doc.get("diferenciaPromedio"))
                    .append("variacion", doc.get("variacionPromedio"))
                    // Información adicional
                    .append("empleadosActual", doc.get("empleadosActual"))
                    .append("empleadosAnterior", doc.get("empleadosAnterior"));

            promedios.add(promedio);
        }

        log.info("Promedios procesados: {}", promedios.size());
        return promedios;
    }

    /**
     * NUEVO MÉTODO: Verifica los datos disponibles para un período
     */
    public Map<String, Object> verificarDatos(String periodoActual) {
        log.info("=== SERVICE: Verificando datos para período {} ===", periodoActual);

        validarPeriodo(periodoActual);

        return repository.verificarDatos(periodoActual);
    }

    /**
     * NUEVO MÉTODO: Valida que el formato del período sea correcto
     */
    public void validarPeriodo(String periodo) {
        if (periodo == null || periodo.trim().isEmpty()) {
            throw new IllegalArgumentException("El período no puede ser nulo o vacío");
        }

        // Validar formato básico YYYYSS (6 dígitos)
        if (!periodo.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "Formato de período inválido: " + periodo + ". Formato esperado: YYYYSS (ej: 202538)"
            );
        }

        try {
            int periodoInt = Integer.parseInt(periodo);
            int año = periodoInt / 100;
            int semana = periodoInt % 100;

            // Validaciones lógicas
            if (año < 2020 || año > 2070) {
                throw new IllegalArgumentException("Año fuera del rango válido (2020-2070): " + año);
            }

            if (semana < 1 || semana > 53) {
                throw new IllegalArgumentException("Semana fuera del rango válido (1-53): " + semana);
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al parsear el período: " + periodo, e);
        }

        log.debug("Período validado correctamente: {}", periodo);
    }
}