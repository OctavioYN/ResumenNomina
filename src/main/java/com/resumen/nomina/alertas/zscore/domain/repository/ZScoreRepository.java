package com.resumen.nomina.alertas.zscore.domain.repository;


import com.resumen.nomina.alertas.zscore.domain.model.ZScoreConfig;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreData;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 📊 REPOSITORIO PARA DATOS Z-SCORE
 */
public interface ZScoreRepository {

    /**
     * Obtiene datos del período actual
     */
    List<ZScoreData> obtenerDatosActuales(String periodo, String sucursal, ZScoreConfig config);

    /**
     * Obtiene datos históricos (excluyendo período actual)
     */
    List<ZScoreData> obtenerDatosHistoricos(String periodo, String sucursal, ZScoreConfig config);

    /**
     * Calcula estadísticas por grupo (puesto + indicador + sucursal + negocio)
     */
    List<Estadistica> calcularEstadisticas(String periodo, String sucursal, ZScoreConfig config);

    /**
     * DTO para estadísticas
     */
    @Data
    @Builder
    class Estadistica {
        private String puesto;
        private String indicador;
        private Integer conceptoDetalle;
        private String sucursal;
        private Integer negocio;
        private Double media;
        private Double desviacion;
        private Integer cantidad;
    }
}