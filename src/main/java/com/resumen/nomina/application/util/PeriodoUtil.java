package com.resumen.nomina.application.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Slf4j
@Component
public class PeriodoUtil {

    /**
     * Calcula el período anterior basándose en el formato YYYYSS (Año + Semana)
     * @param periodoActual Período actual en formato YYYYSS
     * @return Período anterior en formato YYYYSS
     */
    public String calcularPeriodoAnterior(String periodoActual) {
        try {
            // Extraer año y semana del formato YYYYSS (ej: 202537 -> año=2025, semana=37)
            int periodo = Integer.parseInt(periodoActual);
            int año = periodo / 100;
            int semana = periodo % 100;

            log.debug("Período actual: {} -> Año: {}, Semana: {}", periodoActual, año, semana);

            // Calcular período anterior
            int añoAnterior = año;
            int semanaAnterior = semana - 1;

            // Si la semana es 1, ir al año anterior y obtener la última semana del año
            if (semanaAnterior == 0) {
                añoAnterior = año - 1;
                semanaAnterior = obtenerUltimaSemanaDelAño(añoAnterior);
            }

            String periodoAnterior = formatearPeriodo(añoAnterior, semanaAnterior);
            log.debug("Período anterior calculado: {} -> Año: {}, Semana: {}",
                    periodoAnterior, añoAnterior, semanaAnterior);

            return periodoAnterior;

        } catch (NumberFormatException e) {
            log.error("Error al parsear el período: {}", periodoActual, e);
            throw new IllegalArgumentException("Formato de período inválido: " + periodoActual + ". Formato esperado: YYYYSS");
        }
    }

    /**
     * Obtiene la última semana del año especificado
     * @param año Año para calcular la última semana
     * @return Número de la última semana del año
     */
    private int obtenerUltimaSemanaDelAño(int año) {
        try {
            // Obtener el último día del año
            LocalDate ultimoDiaDelAño = LocalDate.of(año, 12, 31);

            // Calcular la semana del año usando ISO-8601 (lunes como primer día)
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int ultimaSemana = ultimoDiaDelAño.get(weekFields.weekOfYear());

            log.debug("Última semana del año {}: {}", año, ultimaSemana);
            return ultimaSemana;

        } catch (Exception e) {
            log.warn("Error al calcular última semana del año {}, usando 52 por defecto", año, e);
            return 52; // Fallback a 52 semanas
        }
    }

    /**
     * Formatea el año y semana en formato YYYYSS
     * @param año Año
     * @param semana Semana
     * @return Período formateado
     */
    private String formatearPeriodo(int año, int semana) {
        return String.format("%d%02d", año, semana);
    }

    /**
     * Valida si el formato del período es correcto
     * @param periodo Período a validar
     * @return true si el formato es válido
     */
    public boolean validarFormatoPeriodo(String periodo) {
        if (periodo == null || periodo.length() != 6) {
            return false;
        }

        try {
            int periodoInt = Integer.parseInt(periodo);
            int año = periodoInt / 100;
            int semana = periodoInt % 100;

            // Validaciones básicas
            return año >= 2000 && año <= 2099 && semana >= 1 && semana <= 53;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extrae el año del período
     * @param periodo Período en formato YYYYSS
     * @return Año
     */
    public int extraerAño(String periodo) {
        if (!validarFormatoPeriodo(periodo)) {
            throw new IllegalArgumentException("Formato de período inválido: " + periodo);
        }
        return Integer.parseInt(periodo) / 100;
    }

    /**
     * Extrae la semana del período
     * @param periodo Período en formato YYYYSS
     * @return Semana
     */
    public int extraerSemana(String periodo) {
        if (!validarFormatoPeriodo(periodo)) {
            throw new IllegalArgumentException("Formato de período inválido: " + periodo);
        }
        return Integer.parseInt(periodo) % 100;
    }
}