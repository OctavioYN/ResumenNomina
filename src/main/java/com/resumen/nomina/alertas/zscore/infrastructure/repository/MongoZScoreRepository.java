package com.resumen.nomina.alertas.zscore.infrastructure.repository;

import com.mongodb.client.MongoCollection;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreConfig;
import com.resumen.nomina.alertas.zscore.domain.model.ZScoreData;
import com.resumen.nomina.alertas.zscore.domain.repository.ZScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 📊 IMPLEMENTACIÓN MONGODB - CORREGIDA
 * NORMALIZA TODOS LOS VALORES A DECIMAL EN LA QUERY
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MongoZScoreRepository implements ZScoreRepository {

    private final MongoTemplate mongoTemplate;
    private static final String COLECCION = "IndicadoresCalculados";

    @Override
    public List<ZScoreData> obtenerDatosActuales(String periodo, String sucursal, ZScoreConfig config) {

        log.info("📋 Obteniendo datos actuales - Período: {}", periodo);

        Document filtro = new Document("periodoActual", periodo);

        // ✅ NO aplicar filtro de outliers en datos actuales
        // Solo filtrar concepto excluido
        if (config.getConceptoExcluir() != null) {
            filtro.append("conceptoDetalle", new Document("$ne", config.getConceptoExcluir()));
        }

        // Filtro de sucursal
        if (sucursal != null && !sucursal.trim().isEmpty() && !"TODAS".equalsIgnoreCase(sucursal)) {
            filtro.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        List<Document> pipeline = Arrays.asList(
                new Document("$match", filtro),

                // 🔴 NORMALIZAR variacion a decimal en la query
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("periodo", "$periodoActual")
                        .append("variacion", new Document("$cond", Arrays.asList(
                                // Si |variacion| > 1, está en porcentaje → dividir entre 100
                                new Document("$gt", Arrays.asList(
                                        new Document("$abs", "$variacion"), 1
                                )),
                                new Document("$divide", Arrays.asList("$variacion", 100)),
                                "$variacion"
                        )))
                )
        );

        return ejecutarPipeline(pipeline).stream()
                .map(this::mapearAZScoreData)
                .collect(Collectors.toList());
    }

    @Override
    public List<ZScoreData> obtenerDatosHistoricos(String periodo, String sucursal, ZScoreConfig config) {

        log.info("📚 Obteniendo datos históricos - Excluyendo: {}", periodo);

        Document filtro = new Document("periodoActual", new Document("$ne", periodo));
        aplicarFiltros(filtro, sucursal, config);

        List<Document> pipeline = Arrays.asList(
                new Document("$match", filtro),

                // 🔴 NORMALIZAR variacion a decimal en la query
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("periodo", "$periodoActual")
                        .append("variacion", new Document("$cond", Arrays.asList(
                                new Document("$gt", Arrays.asList(
                                        new Document("$abs", "$variacion"), 1
                                )),
                                new Document("$divide", Arrays.asList("$variacion", 100)),
                                "$variacion"
                        )))
                )
        );

        return ejecutarPipeline(pipeline).stream()
                .map(this::mapearAZScoreData)
                .collect(Collectors.toList());
    }

    @Override
    public List<Estadistica> calcularEstadisticas(String periodo, String sucursal, ZScoreConfig config) {

        log.info("📊 Calculando estadísticas - Período excluido: {}", periodo);

        Document filtro = new Document("periodoActual", new Document("$ne", periodo));
        aplicarFiltros(filtro, sucursal, config);

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar
                new Document("$match", filtro),

                // 2. 🔴 NORMALIZAR variacion ANTES de agrupar (CRÍTICO)
                new Document("$project", new Document()
                        .append("fcDetalle5", 1)
                        .append("fcDetalle6", 1)
                        .append("conceptoDetalle", 1)
                        .append("sucursal", 1)
                        .append("negocio", 1)
                        .append("variacionNormalizada", new Document("$cond", Arrays.asList(
                                // Si |variacion| > 1, dividir entre 100
                                new Document("$gt", Arrays.asList(
                                        new Document("$abs", "$variacion"), 1
                                )),
                                new Document("$divide", Arrays.asList("$variacion", 100)),
                                "$variacion"
                        )))
                ),

                // 3. Agrupar con variacion normalizada
                new Document("$group", new Document("_id", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio"))
                        .append("media", new Document("$avg", "$variacionNormalizada"))
                        .append("desviacion", new Document("$stdDevPop", "$variacionNormalizada"))
                        .append("cantidad", new Document("$sum", 1))
                ),

                // 4. Filtrar por cantidad mínima
                new Document("$match", new Document("cantidad",
                        new Document("$gte", config.getPeriodosMinimos()))),

                // 5. Proyectar resultado
                new Document("$project", new Document()
                        .append("_id", 0)
                        .append("puesto", "$_id.puesto")
                        .append("indicador", "$_id.indicador")
                        .append("conceptoDetalle", "$_id.conceptoDetalle")
                        .append("sucursal", "$_id.sucursal")
                        .append("negocio", "$_id.negocio")
                        .append("media", 1)
                        .append("desviacion", 1)
                        .append("cantidad", 1)
                )
        );

        List<Document> results = ejecutarPipeline(pipeline);

        // Log de diagnóstico CORREGIDO
        if (!results.isEmpty()) {
            double avgDesv = results.stream()
                    .mapToDouble(d -> Math.abs(d.getDouble("desviacion")))
                    .average()
                    .orElse(0);

            log.info("📊 Estadísticas calculadas: {} grupos", results.size());
            log.info("📊 Desviación promedio: {} ({}%)",
                    String.format("%.4f", avgDesv),
                    String.format("%.2f", avgDesv * 100));

            if (avgDesv > 0.50) {
                log.warn("⚠️ Desviación promedio muy alta (> 50%)");
                log.warn("   Esto puede indicar outliers extremos en los datos");
            }

            // 🔴 LOG ADICIONAL: Ver algunos ejemplos
            log.info("📊 Primeros 5 grupos con sus estadísticas:");
            results.stream().limit(5).forEach(doc -> {
                log.info("   - {}: media={}, σ={}, n={}",
                        doc.getString("indicador").substring(0, Math.min(30, doc.getString("indicador").length())),
                        String.format("%.4f", doc.getDouble("media")),
                        String.format("%.4f", doc.getDouble("desviacion")),
                        doc.getInteger("cantidad"));
            });
        }

        return results.stream()
                .map(this::mapearAEstadistica)
                .collect(Collectors.toList());
    }

    /**
     * Aplica filtros comunes
     */
    private void aplicarFiltros(Document filtro, String sucursal, ZScoreConfig config) {

        // Filtro de sucursal
        if (sucursal != null && !sucursal.trim().isEmpty() && !"TODAS".equalsIgnoreCase(sucursal)) {
            filtro.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        // Excluir concepto (ej: 1011 = Empleados)
        if (config.getConceptoExcluir() != null) {
            filtro.append("conceptoDetalle", new Document("$ne", config.getConceptoExcluir()));
        }

        // 🔴 EXCLUIR outliers extremos (valores > 1000% o < -1000%)
        filtro.append("variacion", new Document("$gte", -10.0).append("$lte", 10.0));
    }

    /**
     * Ejecuta pipeline de agregación
     */
    private List<Document> ejecutarPipeline(List<Document> pipeline) {
        try {
            MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(COLECCION);
            List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());
            log.debug("✅ Pipeline ejecutado - Resultados: {}", results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ Error en pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("Error en MongoDB: " + e.getMessage(), e);
        }
    }

    /**
     * Mapea Document a ZScoreData
     */
    private ZScoreData mapearAZScoreData(Document doc) {
        return ZScoreData.builder()
                .puesto(doc.getString("puesto"))
                .indicador(doc.getString("indicador"))
                .conceptoDetalle(doc.getInteger("conceptoDetalle"))
                .sucursal(doc.getString("sucursal"))
                .negocio(doc.getInteger("negocio"))
                .periodo(doc.getString("periodo"))
                .variacion(doc.getDouble("variacion"))  // Ya normalizada
                .build();
    }

    /**
     * Mapea Document a Estadistica
     */
    private Estadistica mapearAEstadistica(Document doc) {
        return Estadistica.builder()
                .puesto(doc.getString("puesto"))
                .indicador(doc.getString("indicador"))
                .conceptoDetalle(doc.getInteger("conceptoDetalle"))
                .sucursal(doc.getString("sucursal"))
                .negocio(doc.getInteger("negocio"))
                .media(doc.getDouble("media"))           // Ya en decimal
                .desviacion(doc.getDouble("desviacion")) // Ya en decimal
                .cantidad(doc.getInteger("cantidad"))
                .build();
    }
}