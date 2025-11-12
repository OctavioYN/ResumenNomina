package com.resumen.nomina.alertas.zscore.infrastructure.repository;


import com.mongodb.client.MongoCollection;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreConfig;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreData;
import com.resumen.nomina.alertas.zscore.domain.repository.ZScoreCalculatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 📊 IMPLEMENTACIÓN MONGODB PARA CÁLCULO Z-SCORE
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MongoZScoreCalculatorRepository implements ZScoreCalculatorRepository {

    private final MongoTemplate mongoTemplate;

    private static final String COLECCION_INDICADORES = "IndicadoresCalculados";

    @Override
    public EstadisticasHistoricas calcularEstadisticasHistoricas(String periodoActual, String sucursal,
                                                                 Integer negocio, ZScoreConfig config) {
        log.info("📊 Calculando estadísticas históricas Z-Score - Período: {}, Sucursal: {}",
                periodoActual, sucursal);

        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(COLECCION_INDICADORES);

        List<Document> pipeline = construirPipelineEstadisticas(periodoActual, sucursal, negocio, config);
        List<Document> results = ejecutarPipeline(collection, pipeline);

        return convertirAEstadisticas(results);
    }

    @Override
    public List<ZScoreData> obtenerDatosActuales(String periodoActual, String sucursal,
                                                 Integer negocio, ZScoreConfig config) {
        log.info("📋 Obteniendo datos actuales Z-Score - Período: {}", periodoActual);

        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(COLECCION_INDICADORES);

        List<Document> pipeline = Arrays.asList(
                new Document("$match", construirFiltroBase(periodoActual, sucursal, negocio, config, false)),
                new Document("$match", new Document("periodoActual", periodoActual)),
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("periodo", "$periodoActual")
                        .append("variacion", 1)
                        .append("valorActual", 1)
                )
        );

        List<Document> results = ejecutarPipeline(collection, pipeline);
        return convertirAZScoreData(results);
    }

    @Override
    public List<ZScoreData> obtenerDatosHistoricos(String periodoActual, String sucursal,
                                                   Integer negocio, ZScoreConfig config) {
        log.info("📚 Obteniendo datos históricos Z-Score - Período: {}", periodoActual);

        MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(COLECCION_INDICADORES);

        List<Document> pipeline = Arrays.asList(
                new Document("$match", construirFiltroBase(periodoActual, sucursal, negocio, config, true)),
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("periodo", "$periodoActual")
                        .append("variacion", 1)
                        .append("valorActual", 1)
                )
        );

        List<Document> results = ejecutarPipeline(collection, pipeline);
        return convertirAZScoreData(results);
    }

    // ========== MÉTODOS PRIVADOS ==========

    private List<Document> construirPipelineEstadisticas(String periodoActual, String sucursal,
                                                         Integer negocio, ZScoreConfig config) {
        return Arrays.asList(
                new Document("$match", construirFiltroBase(periodoActual, sucursal, negocio, config, true)),

                new Document("$group", new Document("_id", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio"))
                        .append("variacionMedia", new Document("$avg", "$variacion"))
                        .append("desviacionEstandar", new Document("$stdDevPop", "$variacion"))
                        .append("cantidadPeriodos", new Document("$sum", 1))
                ),

                new Document("$match", new Document("cantidadPeriodos",
                        new Document("$gte", config.getPeriodosMinimosHistoricos()))),

                new Document("$project", new Document("_id", 0)
                        .append("puesto", "$_id.puesto")
                        .append("indicador", "$_id.indicador")
                        .append("conceptoDetalle", "$_id.conceptoDetalle")
                        .append("sucursal", "$_id.sucursal")
                        .append("negocio", "$_id.negocio")
                        .append("variacionMedia", 1)
                        .append("desviacionEstandar", 1)
                        .append("cantidadPeriodos", 1)
                )
        );
    }

    private Document construirFiltroBase(String periodoActual, String sucursal, Integer negocio,
                                         ZScoreConfig config, boolean excluirActual) {
        Document filtro = new Document();

        if (excluirActual) {
            filtro.append("periodoActual", new Document("$ne", periodoActual));
        }

        if (sucursal != null && !sucursal.trim().isEmpty() && !"TODAS".equalsIgnoreCase(sucursal)) {
            filtro.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        if (negocio != null && negocio > 0) {
            filtro.append("negocio", negocio);
        }

        // Aplicar exclusiones de configuración
        if (config.getConceptosExcluidos() != null && !config.getConceptosExcluidos().isEmpty()) {
            filtro.append("conceptoDetalle", new Document("$nin", config.getConceptosExcluidos()));
        }

        if (config.getNegociosExcluidos() != null && !config.getNegociosExcluidos().isEmpty()) {
            filtro.append("negocio", new Document("$nin", config.getNegociosExcluidos()));
        }

        if (config.getPuestosExcluidos() != null && !config.getPuestosExcluidos().isEmpty()) {
            filtro.append("fcDetalle5", new Document("$nin", config.getPuestosExcluidos()));
        }

        return filtro;
    }

    private List<ZScoreData> convertirAZScoreData(List<Document> documentos) {
        return documentos.stream()
                .map(doc -> ZScoreData.builder()
                        .puesto(doc.getString("puesto"))
                        .indicador(doc.getString("indicador"))
                        .conceptoDetalle(doc.getInteger("conceptoDetalle"))
                        .sucursal(doc.getString("sucursal"))
                        .negocio(doc.getInteger("negocio"))
                        .periodo(doc.getString("periodo"))
                        .variacion(doc.getDouble("variacion"))
                        .valorActual(doc.getDouble("valorActual"))
                        .fechaCalculo(LocalDateTime.now())
                        .fuenteDatos("IndicadoresCalculados")
                        .build())
                .collect(Collectors.toList());
    }

    private EstadisticasHistoricas convertirAEstadisticas(List<Document> resultados) {
        return new EstadisticasHistoricas() {
            @Override
            public List<EstadisticaPuesto> getEstadisticas() {
                return resultados.stream()
                        .map(doc -> new EstadisticaPuesto() {
                            public String getPuesto() { return doc.getString("puesto"); }
                            public String getIndicador() { return doc.getString("indicador"); }
                            public Integer getConceptoDetalle() { return doc.getInteger("conceptoDetalle"); }
                            public String getSucursal() { return doc.getString("sucursal"); }
                            public Integer getNegocio() { return doc.getInteger("negocio"); }
                            public double getVariacionMedia() { return doc.getDouble("variacionMedia"); }
                            public double getDesviacionEstandar() { return doc.getDouble("desviacionEstandar"); }
                            public int getCantidadPeriodos() { return doc.getInteger("cantidadPeriodos"); }
                        })
                        .collect(Collectors.toList());
            }
        };
    }

    private List<Document> ejecutarPipeline(MongoCollection<Document> collection, List<Document> pipeline) {
        try {
            List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());
            log.info("✅ Pipeline ejecutado exitosamente. Resultados: {}", results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ Error ejecutando pipeline MongoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Error en agregación MongoDB: " + e.getMessage(), e);
        }
    }
}