package com.resumen.nomina.alertas.arima.infrastructure.repository;


import com.mongodb.client.MongoCollection;
import com.resumen.nomina.alertas.arima.domain.model.ArimaConfig;
import com.resumen.nomina.alertas.arima.domain.model.ArimaData;
import com.resumen.nomina.alertas.arima.domain.repository.ArimaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
        import java.util.stream.Collectors;

/**
 * 📊 IMPLEMENTACIÓN MONGODB ARIMA
 *
 * Obtiene series temporales de MongoDB para análisis ARIMA
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MongoArimaRepository implements ArimaRepository {

    private final MongoTemplate mongoTemplate;
    private static final String COLECCION = "IndicadoresCalculados";

    @Override
    public Map<String, List<ArimaData>> obtenerSeriesTemporales(
            String periodoActual, String sucursal, ArimaConfig config) {

        log.info("📊 Obteniendo series temporales - Período actual excluido: {}", periodoActual);

        // Filtro: excluir período actual
        Document filtro = new Document("periodoActual", new Document("$ne", periodoActual));
        aplicarFiltros(filtro, sucursal, config);

        // Pipeline de agregación CORREGIDO
        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar
                new Document("$match", filtro),

                // 2. PROYECCIÓN CORREGIDA - NO normalizar automáticamente
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("periodo", "$periodoActual")
                        // USAR EL VALOR ORIGINAL SIN NORMALIZAR
                        .append("valor", "$variacion") // ← CAMBIO CLAVE: usar variacion directamente
                ),

                // 3. Ordenar por período
                new Document("$sort", new Document("periodo", 1))
        );

        List<Document> results = ejecutarPipeline(pipeline);
        log.info("📊 Documentos obtenidos: {}", results.size());

        // Agrupar por clave
        Map<String, List<ArimaData>> series = results.stream()
                .map(this::mapearAArimaData)
                .collect(Collectors.groupingBy(
                        ArimaData::getClave,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Filtrar series con suficientes datos
        Map<String, List<ArimaData>> seriesFiltradas = series.entrySet().stream()
                .filter(e -> e.getValue().size() >= config.getPeriodosMinimos())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        log.info("📊 Series temporales válidas: {} (con ≥ {} períodos)",
                seriesFiltradas.size(), config.getPeriodosMinimos());

        return seriesFiltradas;
    }

    @Override
    public List<ArimaData> obtenerDatosActuales(String periodo, String sucursal, ArimaConfig config) {

        log.info("📋 Obteniendo datos actuales - Período: {}", periodo);

        Document filtro = new Document("periodoActual", periodo);

        if (sucursal != null && !sucursal.trim().isEmpty() && !"TODAS".equalsIgnoreCase(sucursal)) {
            filtro.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        List<Document> pipeline = Arrays.asList(
                new Document("$match", filtro),
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("periodo", "$periodoActual")
                        // USAR VALOR ORIGINAL
                        .append("valor", "$variacion") // ← CAMBIO CLAVE
                )
        );

        List<Document> results = ejecutarPipeline(pipeline);
        log.info("📋 Datos actuales obtenidos: {}", results.size());

        return results.stream()
                .map(this::mapearAArimaData)
                .collect(Collectors.toList());
    }



    /**
     * Aplica filtros comunes
     */
    private void aplicarFiltros(Document filtro, String sucursal, ArimaConfig config) {

        // Filtro de sucursal
        if (sucursal != null && !sucursal.trim().isEmpty() &&
                !"TODAS".equalsIgnoreCase(sucursal)) {
            filtro.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        // Excluir concepto específico
        if (config.getConceptoExcluir() != null) {
            filtro.append("conceptoDetalle", new Document("$ne", config.getConceptoExcluir()));
        }

        // Excluir outliers extremos (> ±1000%)
        filtro.append("variacion", new Document("$gte", -10.0).append("$lte", 10.0));
    }

    /**
     * Ejecuta pipeline de agregación
     */
    private List<Document> ejecutarPipeline(List<Document> pipeline) {
        try {
            MongoCollection<Document> collection =
                    mongoTemplate.getDb().getCollection(COLECCION);
            List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());
            log.debug("✅ Pipeline ejecutado - Resultados: {}", results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ Error en pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("Error en MongoDB: " + e.getMessage(), e);
        }
    }

    /**
     * Mapea Document a ArimaData
     */
    private ArimaData mapearAArimaData(Document doc) {
        return ArimaData.builder()
                .puesto(doc.getString("puesto"))
                .indicador(doc.getString("indicador"))
                .conceptoDetalle(doc.getInteger("conceptoDetalle"))
                .sucursal(doc.getString("sucursal"))
                .negocio(doc.getInteger("negocio"))
                .periodo(doc.getString("periodo"))
                .valor(doc.getDouble("valor"))
                .build();
    }
}