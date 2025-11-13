/*package com.resumen.nomina.application.service;

import com.resumen.nomina.domain.model.Indicador;
import com.resumen.nomina.application.repository.CalculoIndicadorRepository;
import com.resumen.nomina.presentation.controller.CalculoIndicadorController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculoIndicadorService {

    private final CalculoIndicadorRepository indicadorRepository;
    private static final Logger logger = LoggerFactory.getLogger(CalculoIndicadorController.class);


    public List<Indicador> obtenerIndicadores(List<String> periodos) {
        return indicadorRepository.obtenerIndicadoresPorPeriodos(periodos);
    }
}
*/
package com.resumen.nomina.application.service;

import com.resumen.nomina.domain.model.Indicador;
import com.resumen.nomina.domain.model.IndicadorCalculado;
import com.resumen.nomina.infrastructure.repository.CalculoIndicadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalculoIndicadorService {

    private static final Logger logger = LoggerFactory.getLogger(CalculoIndicadorService.class);

    private final CalculoIndicadorRepository calculoIndicadorRepository;

    @Autowired
    public CalculoIndicadorService(CalculoIndicadorRepository calculoIndicadorRepository) {
        this.calculoIndicadorRepository = calculoIndicadorRepository;
    }

    /**
     * Ejecuta el cálculo de indicadores sin guardar (solo consulta)
     */
    public List<Indicador> ejecutarCalculoConsulta(List<String> periodos) {
        logger.info("Ejecutando cálculo de consulta para periodos: {}", periodos);

        if (periodos == null || periodos.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un periodo");
        }

        if (periodos.size() != 2) {
            throw new IllegalArgumentException("Se requieren exactamente 2 periodos para el cálculo");
        }

        return calculoIndicadorRepository.obtenerIndicadoresPorPeriodos(periodos);
    }

    /**
     * Ejecuta el cálculo y guarda los resultados
     */
    public List<IndicadorCalculado> ejecutarCalculoYGuardar(List<String> periodos, String usuario) {
        logger.info("Ejecutando cálculo y guardado para periodos: {} por usuario: {}", periodos, usuario);

        validarParametrosCalculo(periodos, usuario);

        return calculoIndicadorRepository.calcularYGuardarIndicadores(periodos, usuario);
    }

    /**
     * Recalcula y reemplaza cálculos existentes
     */
    public List<IndicadorCalculado> recalcularIndicadores(List<String> periodos, String usuario) {
        logger.info("Ejecutando recálculo para periodos: {} por usuario: {}", periodos, usuario);

        validarParametrosCalculo(periodos, usuario);

        return calculoIndicadorRepository.recalcularYReemplazarIndicadores(periodos, usuario);
    }

    /**
     * Obtiene todos los indicadores calculados guardados
     */
    public List<IndicadorCalculado> obtenerTodosLosCalculos() {
        logger.info("Obteniendo todos los cálculos guardados");
        return calculoIndicadorRepository.obtenerIndicadoresCalculadosGuardados();
    }

    /**
     * Obtiene indicadores calculados por periodo
     */
    public List<IndicadorCalculado> obtenerCalculosPorPeriodo(String periodo) {
        logger.info("Obteniendo cálculos para periodo: {}", periodo);

        if (periodo == null || periodo.trim().isEmpty()) {
            throw new IllegalArgumentException("El periodo no puede estar vacío");
        }

        return calculoIndicadorRepository.obtenerIndicadoresCalculadosPorPeriodo(periodo);
    }

    /**
     * Obtiene indicadores calculados por negocio
     */
    public List<IndicadorCalculado> obtenerCalculosPorNegocio(Integer negocio) {
        logger.info("Obteniendo cálculos para negocio: {}", negocio);

        if (negocio == null) {
            throw new IllegalArgumentException("El negocio no puede ser nulo");
        }

        return calculoIndicadorRepository.obtenerIndicadoresCalculadosPorNegocio(negocio);
    }

    /**
     * Obtiene estadísticas de los cálculos
     */
    public Map<String, Object> obtenerEstadisticasCalculos() {
        logger.info("Generando estadísticas de cálculos");

        List<IndicadorCalculado> todosLosCalculos = calculoIndicadorRepository.obtenerIndicadoresCalculadosGuardados();

        Map<String, Long> calculosPorNegocio = todosLosCalculos.stream()
                .collect(Collectors.groupingBy(
                        calc -> calc.getNegocio().toString(),
                        Collectors.counting()
                ));

        Map<String, Long> calculosPorPeriodo = todosLosCalculos.stream()
                .collect(Collectors.groupingBy(
                        IndicadorCalculado::getPeriodoActual,
                        Collectors.counting()
                ));

        Map<String, Long> calculosPorTipo = todosLosCalculos.stream()
                .collect(Collectors.groupingBy(
                        IndicadorCalculado::getTipoCalculo,
                        Collectors.counting()
                ));

        LocalDateTime fechaMasReciente = todosLosCalculos.stream()
                .map(IndicadorCalculado::getFechaCalculo)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return Map.of(
                "totalCalculos", todosLosCalculos.size(),
                "calculosPorNegocio", calculosPorNegocio,
                "calculosPorPeriodo", calculosPorPeriodo,
                "calculosPorTipo", calculosPorTipo,
                "fechaUltimoCalculo", fechaMasReciente,
                "periodosDisponibles", calculosPorPeriodo.keySet()
        );
    }

    /**
     * Limpia cálculos antiguos (anterior a N días)
     */
    public void limpiarCalculosAntiguos(int diasAntiguedad) {
        logger.info("Iniciando limpieza de cálculos anteriores a {} días", diasAntiguedad);

        if (diasAntiguedad <= 0) {
            throw new IllegalArgumentException("Los días de antigüedad deben ser mayor a 0");
        }

        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);
        calculoIndicadorRepository.limpiarCalculosAnteriores(fechaLimite);

        logger.info("Limpieza completada para cálculos anteriores a: {}", fechaLimite);
    }

    /**
     * Obtiene resumen de cálculos recientes
     */
    public Map<String, Object> obtenerResumenReciente() {
        logger.info("Generando resumen de cálculos recientes");

        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);

        List<IndicadorCalculado> todosLosCalculos = calculoIndicadorRepository.obtenerIndicadoresCalculadosGuardados();

        long calculosUltimas24h = todosLosCalculos.stream()
                .filter(calc -> calc.getFechaCalculo().isAfter(hace24Horas))
                .count();

        long calculosUltimos7dias = todosLosCalculos.stream()
                .filter(calc -> calc.getFechaCalculo().isAfter(hace7Dias))
                .count();

        return Map.of(
                "calculosUltimas24h", calculosUltimas24h,
                "calculosUltimos7dias", calculosUltimos7dias,
                "totalCalculos", todosLosCalculos.size(),
                "fechaConsulta", LocalDateTime.now()
        );
    }

    // Métodos privados de validación

    private void validarParametrosCalculo(List<String> periodos, String usuario) {
        if (periodos == null || periodos.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un periodo");
        }

        if (periodos.size() != 2) {
            throw new IllegalArgumentException("Se requieren exactamente 2 periodos para el cálculo");
        }

        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }

        // Validar formato de periodos (opcional, según tu formato)
        for (String periodo : periodos) {
            if (periodo == null || periodo.trim().isEmpty()) {
                throw new IllegalArgumentException("Los periodos no pueden estar vacíos");
            }
        }
    }
}