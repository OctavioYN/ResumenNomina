package com.resumen.nomina.alertas.zscore.domain.repository;

import com.resumen.nomina.alertas.zscore.domain.model.ZScoreConfig;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreData;

import java.util.List;

/**
 * 📊 REPOSITORIO PARA CÁLCULO DE DATOS Z-SCORE
 */
public interface ZScoreCalculatorRepository {

    /**
     * Obtiene datos históricos para cálculo de estadísticas
     */
    List<ZScoreData> obtenerDatosHistoricos(String periodoActual, String sucursal,
                                            Integer negocio, ZScoreConfig config);

    /**
     * Obtiene datos del período actual para evaluación
     */
    List<ZScoreData> obtenerDatosActuales(String periodoActual, String sucursal,
                                          Integer negocio, ZScoreConfig config);

    /**
     * Calcula estadísticas históricas por puesto/indicador
     */
    EstadisticasHistoricas calcularEstadisticasHistoricas(String periodoActual, String sucursal,
                                                          Integer negocio, ZScoreConfig config);

    /**
     * DTO para resultados de estadísticas históricas
     */
    interface EstadisticasHistoricas {
        List<EstadisticaPuesto> getEstadisticas();

        interface EstadisticaPuesto {
            String getPuesto();
            String getIndicador();
            Integer getConceptoDetalle();
            String getSucursal();
            Integer getNegocio();
            double getVariacionMedia();
            double getDesviacionEstandar();
            int getCantidadPeriodos();
        }
    }
}