package com.resumen.nomina.application.service;



import com.resumen.nomina.infrastructure.repository.IndicadoresGeneralesInfrastructureRepository;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IndicadoresGeneralesService {

    private static final Logger logger = LoggerFactory.getLogger(IndicadoresGeneralesService.class);

    private final IndicadoresGeneralesInfrastructureRepository infraRepository;

    @Autowired
    public IndicadoresGeneralesService(IndicadoresGeneralesInfrastructureRepository infraRepository) {
        this.infraRepository = infraRepository;
    }

    /**
     * Obtiene todos los indicadores para un período específico
     */
    public List<Document> obtenerTodosLosIndicadores(String periodoActual) {
        logger.info("Obteniendo todos los indicadores para período: {}", periodoActual);

        try {
            List<Document> resultados = infraRepository.obtenerIndicadoresPorPeriodo(periodoActual);
            logger.info("Se obtuvieron {} indicadores para período {}", resultados.size(), periodoActual);
            return resultados;

        } catch (Exception e) {
            logger.error("Error obteniendo indicadores para período {}: {}", periodoActual, e.getMessage(), e);
            throw new RuntimeException("Error al obtener indicadores: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene indicadores filtrados por sucursal
     */
    public List<Document> obtenerIndicadoresPorSucursal(String periodoActual, String sucursal) {
        logger.info("Obteniendo indicadores por sucursal: {} para período: {}", sucursal, periodoActual);

        try {
            List<Document> resultados = infraRepository.obtenerIndicadoresPorSucursal(periodoActual, sucursal);
            logger.info("Se obtuvieron {} indicadores para sucursal {} en período {}",
                    resultados.size(), sucursal, periodoActual);
            return resultados;

        } catch (Exception e) {
            logger.error("Error obteniendo indicadores por sucursal: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener indicadores por sucursal: " + e.getMessage(), e);
        }
    }


    public List<Document> obtenerIndicadoresPorSucursalPuesto(String periodoActual, Integer negocio, Integer puesto) {
        logger.info("Obteniendo indicadores por sucursal: {} para período: {}", negocio, periodoActual);

        try {
            List<Document> resultados = infraRepository.obtenerIndicadoresPorSucursalpuesto(periodoActual, negocio,puesto);
            logger.info("Se obtuvieron {} indicadores para sucursal {} en período {}",
                    resultados.size(), negocio, periodoActual);
            return resultados;

        } catch (Exception e) {
            logger.error("Error obteniendo indicadores por sucursal: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener indicadores por sucursal: " + e.getMessage(), e);
        }
    }


    /**
     * Obtiene indicadores filtrados por rango de variación
     */
    public List<Document> obtenerIndicadoresPorVariacion(String periodoActual,
                                                         Double variacionMinima,
                                                         Double variacionMaxima) {
        logger.info("Obteniendo indicadores por variación - período: {}, min: {}, max: {}",
                periodoActual, variacionMinima, variacionMaxima);

        try {
            List<Document> resultados = infraRepository.obtenerIndicadoresPorRangoVariacion(
                    periodoActual, variacionMinima, variacionMaxima);
            logger.info("Se obtuvieron {} indicadores en rango de variación para período {}",
                    resultados.size(), periodoActual);
            return resultados;

        } catch (Exception e) {
            logger.error("Error obteniendo indicadores por variación: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener indicadores por variación: " + e.getMessage(), e);
        }
    }

    /**
     * Validar formato de período
     */
    public void validarPeriodo(String periodo) {
        if (periodo == null || periodo.length() != 6) {
            throw new IllegalArgumentException("Formato de período inválido. Debe ser YYYYWW, ejemplo: 202538");
        }

        try {
            int anio = Integer.parseInt(periodo.substring(0, 4));
            int semana = Integer.parseInt(periodo.substring(4, 6));

            if (anio < 2020 || anio > 2070) {
                throw new IllegalArgumentException("Año debe estar entre 2020 y 2070");
            }

            if (semana < 1 || semana > 53) {
                throw new IllegalArgumentException("Semana debe estar entre 01 y 53");
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de período inválido: " + periodo);
        }
    }

    /**
     * Obtiene estadísticas generales de un período
     */
    public Map<String, Object> obtenerEstadisticasPeriodo(String periodoActual) {
        logger.info("Obteniendo estadísticas generales para período: {}", periodoActual);

        try {
            return infraRepository.obtenerEstadisticasPorPeriodo(periodoActual);

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener estadísticas: " + e.getMessage(), e);
        }
    }

    // Agregar estos 2 métodos a IndicadoresGeneralesService.java

    /**
     * Obtiene indicadores con alertas y estadísticas calculadas
     */
    public List<Document> obtenerIndicadoresConAlertas(String periodoActual, String sucursal) {
        logger.info("Obteniendo indicadores con alertas para período: {}, sucursal: {}", periodoActual, sucursal);

        try {
            validarPeriodo(periodoActual);

            List<Document> resultados = infraRepository.obtenerIndicadoresConAlertas(periodoActual, sucursal);
            logger.info("Se obtuvieron {} indicadores con alertas para período {}", resultados.size(), periodoActual);
            return resultados;

        } catch (Exception e) {
            logger.error("Error obteniendo indicadores con alertas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener indicadores con alertas: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene estadísticas históricas por puesto e indicador (excluyendo período actual)
     */
    public List<Document> obtenerEstadisticasHistoricas(String periodoActual, String sucursal) {
        logger.info("Obteniendo estadísticas históricas para período: {}, sucursal: {}", periodoActual, sucursal);

        try {
            if (periodoActual != null) {
                validarPeriodo(periodoActual);
            }

            List<Document> resultados = infraRepository.obtenerEstadisticasHistoricas(periodoActual, sucursal);
            logger.info("Se obtuvieron estadísticas para {} combinaciones de puesto-indicador", resultados.size());
            return resultados;

        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas históricas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener estadísticas históricas: " + e.getMessage(), e);
        }
    }
}
