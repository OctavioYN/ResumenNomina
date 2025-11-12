package com.resumen.nomina.alertas.arima.domain.repository;


import com.resumen.nomina.alertas.arima.domain.model.ArimaConfig;
import com.resumen.nomina.alertas.arima.domain.model.ArimaData;

import java.util.List;
import java.util.Map;

/**
 * 📊 REPOSITORIO ARIMA
 *
 * Interface para acceso a datos de series temporales
 */
public interface ArimaRepository {

    /**
     * Obtiene series temporales históricas agrupadas por indicador
     *
     * @param periodoActual Período actual a excluir del histórico
     * @param sucursal Filtro de sucursal (opcional)
     * @param config Configuración
     * @return Map con clave = identificador único, valor = lista ordenada de datos históricos
     */
    Map<String, List<ArimaData>> obtenerSeriesTemporales(
            String periodoActual,
            String sucursal,
            ArimaConfig config);

    /**
     * Obtiene datos del período actual para comparar con pronósticos
     *
     * @param periodo Período actual
     * @param sucursal Filtro de sucursal (opcional)
     * @param config Configuración
     * @return Lista de datos del período actual
     */
    List<ArimaData> obtenerDatosActuales(
            String periodo,
            String sucursal,
            ArimaConfig config);
}