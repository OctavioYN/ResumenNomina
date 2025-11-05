package com.resumen.nomina.application.service;

import com.resumen.nomina.domain.model.SemanaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanaService {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<SemanaResponse> obtenerSemanasAnteriores(String fechaString) {
        try {
            // Parsear la fecha de entrada
            LocalDate fecha = LocalDate.parse(fechaString, INPUT_FORMATTER);

            List<SemanaResponse> semanas = new ArrayList<>();

            // Obtener la semana actual y las 4 anteriores (5 semanas en total)
            for (int i = 0; i < 5; i++) {
                LocalDate fechaSemana = fecha.minusWeeks(i);
                SemanaResponse semana = construirSemanaResponse(fechaSemana);
                semanas.add(semana);
            }

            log.info("Semanas obtenidas correctamente para la fecha: {}", fechaString);
            return semanas;

        } catch (Exception e) {
            log.error("Error al procesar la fecha: {}", fechaString, e);
            throw new IllegalArgumentException("Formato de fecha inválido. Use yyyyMMdd (ejemplo: 20250930)", e);
        }
    }

    private SemanaResponse construirSemanaResponse(LocalDate fecha) {
        // Obtener el año y número de semana según ISO-8601
        int anio = fecha.get(IsoFields.WEEK_BASED_YEAR);
        int numeroSemana = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        // Calcular el primer día de la semana (lunes)
        LocalDate inicioSemana = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Calcular el último día de la semana (domingo)
        LocalDate finSemana = fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return SemanaResponse.builder()
                .anio(anio)
                .numeroSemana(numeroSemana)
                .fechaInicio(inicioSemana.format(OUTPUT_FORMATTER))
                .fechaFin(finSemana.format(OUTPUT_FORMATTER))
                .descripcion(String.format("Semana %d - %d", numeroSemana, anio))
                .semana(String.format("%d%d",anio,numeroSemana))
                .build();
    }
}