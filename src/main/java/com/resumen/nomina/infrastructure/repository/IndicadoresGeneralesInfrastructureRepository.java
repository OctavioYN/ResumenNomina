package com.resumen.nomina.infrastructure.repository;


import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class IndicadoresGeneralesInfrastructureRepository {

    private static final Logger logger = LoggerFactory.getLogger(IndicadoresGeneralesInfrastructureRepository.class);
    private final MongoTemplate mongoTemplate;

    @Autowired
    public IndicadoresGeneralesInfrastructureRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Obtiene todos los indicadores para un período específico
     */
    public List<Document> obtenerIndicadoresPorPeriodo(String periodoActual) {
        logger.info("Ejecutando pipeline de indicadores para período: {}", periodoActual);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por período específico
                new Document("$match", new Document("periodoActual", periodoActual)),

                // 2. Proyección directa con formato requerido
                new Document("$project", new Document("_id", 0)
                        .append("indicador", "$fcDetalle6")
                        .append("sucursal", "$sucursal")
                        .append("fcDetalle5", "$fcDetalle5")
                        .append("fcDetalle6", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("periodoActual", "$periodoActual")
                        .append("valorActual", new Document("$round", Arrays.asList("$valorActual", 2)))
                        .append("periodoAnterior", "$periodoAnterior")
                        .append("valorAnterior", new Document("$round", Arrays.asList("$valorAnterior", 2)))
                        .append("diferenciaPesos", new Document("$round", Arrays.asList("$diferencia", 2)))
                        .append("variacionPorcentual", new Document("$round", Arrays.asList("$variacion", 2)))
                        .append("negocio", "$negocio")
                        .append("puesto", "$puesto")),

                // 3. Ordenar por variación descendente
                new Document("$sort", new Document()
                        .append("variacionPorcentual", -1)
                        .append("sucursal", 1)
                        .append("fcDetalle5", 1))
        );

        return ejecutarPipeline(collection, pipeline);
    }

    /**
     * Obtiene indicadores filtrados por sucursal
     */
    public List<Document> obtenerIndicadoresPorSucursal(String periodoActual, String sucursal) {
        logger.info("Ejecutando pipeline por sucursal: {} para período: {}", sucursal, periodoActual);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por período y sucursal
                new Document("$match", new Document()
                        .append("periodoActual", periodoActual)
                        .append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                                .append("$options", "i"))),

                // 2. Proyección
                new Document("$project", new Document("_id", 0)
                        .append("indicador", "$fcDetalle6")
                        .append("sucursal", "$sucursal")
                        .append("fcDetalle5", "$fcDetalle5")
                        .append("fcDetalle6", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("periodoActual", "$periodoActual")
                        .append("valorActual", new Document("$round", Arrays.asList("$valorActual", 2)))
                        .append("periodoAnterior", "$periodoAnterior")
                        .append("valorAnterior", new Document("$round", Arrays.asList("$valorAnterior", 2)))
                        .append("diferenciaPesos", new Document("$round", Arrays.asList("$diferencia", 2)))
                        .append("variacionPorcentual", new Document("$round", Arrays.asList("$variacion", 2)))),

                // 3. Ordenar
                new Document("$sort", new Document("variacionPorcentual", -1))
        );

        return ejecutarPipeline(collection, pipeline);
    }

    /**
     * Obtiene indicadores filtrados por sucursal y puesto
     */
    public List<Document> obtenerIndicadoresPorSucursalpuesto(String periodoActual, Integer negocio, Integer puesto) {
        logger.info("Ejecutando pipeline por sucursal: {} para período: {}", negocio, periodoActual);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por período y sucursal
                new Document("$match", new Document()
                        .append("periodoActual", periodoActual)
                        .append("negocio", negocio)
                        .append("puesto", puesto)
                        //.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*").append("$options", "i"))
                        //.append("fcDetalle5", new Document("$regex", ".*" + puesto.trim() + ".*").append("$options", "i"))

                ),

                // 2. Proyección
                new Document("$project", new Document("_id", 0)
                        .append("puesto", "$puesto")
                        .append("indicador", "$fcDetalle6")
                        .append("sucursal", "$sucursal")
                        .append("fcDetalle5", "$fcDetalle5")
                        .append("fcDetalle6", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("periodoActual", "$periodoActual")
                        .append("valorActual", new Document("$round", Arrays.asList("$valorActual", 2)))
                        .append("periodoAnterior", "$periodoAnterior")
                        .append("valorAnterior", new Document("$round", Arrays.asList("$valorAnterior", 2)))
                        .append("diferenciaPesos", new Document("$round", Arrays.asList("$diferencia", 2)))
                        .append("variacionPorcentual", new Document("$round", Arrays.asList("$variacion", 2)))),
                        //.append("puesto", "$puesto"),

                // 3. Ordenar
                new Document("$sort", new Document("variacionPorcentual", -1))
        );

        return ejecutarPipeline(collection, pipeline);
    }



    /**
     * Obtiene indicadores por rango de variación
     */
    public List<Document> obtenerIndicadoresPorRangoVariacion(String periodoActual,
                                                              Double variacionMinima,
                                                              Double variacionMaxima) {
        logger.info("Ejecutando pipeline por rango variación: {} a {} para período: {}",
                variacionMinima, variacionMaxima, periodoActual);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        Document matchDocument = new Document("periodoActual", periodoActual);

        // Agregar filtros de variación si están definidos
        if (variacionMinima != null || variacionMaxima != null) {
            Document variacionFilter = new Document();
            if (variacionMinima != null) {
                variacionFilter.append("$gte", variacionMinima);
            }
            if (variacionMaxima != null) {
                variacionFilter.append("$lte", variacionMaxima);
            }
            matchDocument.append("variacion", variacionFilter);
        }

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por período y rango de variación
                new Document("$match", matchDocument),

                // 2. Proyección
                new Document("$project", new Document("_id", 0)
                        .append("indicador", "$fcDetalle6")
                        .append("sucursal", "$sucursal")
                        .append("fcDetalle5", "$fcDetalle5")
                        .append("fcDetalle6", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("periodoActual", "$periodoActual")
                        .append("valorActual", new Document("$round", Arrays.asList("$valorActual", 2)))
                        .append("periodoAnterior", "$periodoAnterior")
                        .append("valorAnterior", new Document("$round", Arrays.asList("$valorAnterior", 2)))
                        .append("diferenciaPesos", new Document("$round", Arrays.asList("$diferencia", 2)))
                        .append("variacionPorcentual", new Document("$round", Arrays.asList("$variacion", 2)))),

                // 3. Ordenar por variación absoluta descendente
                new Document("$sort", new Document("variacionPorcentual", -1))
        );

        return ejecutarPipeline(collection, pipeline);
    }

    /**
     * Obtiene estadísticas generales de un período
     */
    public Map<String, Object> obtenerEstadisticasPorPeriodo(String periodoActual) {
        logger.info("Calculando estadísticas para período: {}", periodoActual);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por período
                new Document("$match", new Document("periodoActual", periodoActual)),

                // 2. Calcular estadísticas
                new Document("$group", new Document("_id", null)
                        .append("totalIndicadores", new Document("$sum", 1))
                        .append("sumaValorActual", new Document("$sum", "$valorActual"))
                        .append("sumaValorAnterior", new Document("$sum", "$valorAnterior"))
                        .append("sumaDiferencia", new Document("$sum", "$diferencia"))
                        .append("promedioVariacion", new Document("$avg", "$variacion"))
                        .append("maxVariacion", new Document("$max", "$variacion"))
                        .append("minVariacion", new Document("$min", "$variacion"))
                        .append("sucursalesUnicas", new Document("$addToSet", "$sucursal"))
                        .append("conceptosUnicos", new Document("$addToSet", "$conceptoDetalle"))),

                // 3. Proyección final
                new Document("$project", new Document("_id", 0)
                        .append("periodoActual", periodoActual)
                        .append("totalIndicadores", "$totalIndicadores")
                        .append("valorTotalActual", new Document("$round", Arrays.asList("$sumaValorActual", 2)))
                        .append("valorTotalAnterior", new Document("$round", Arrays.asList("$sumaValorAnterior", 2)))
                        .append("diferenciaTotal", new Document("$round", Arrays.asList("$sumaDiferencia", 2)))
                        .append("promedioVariacion", new Document("$round", Arrays.asList("$promedioVariacion", 2)))
                        .append("maxVariacion", new Document("$round", Arrays.asList("$maxVariacion", 2)))
                        .append("minVariacion", new Document("$round", Arrays.asList("$minVariacion", 2)))
                        .append("cantidadSucursales", new Document("$size", "$sucursalesUnicas"))
                        .append("cantidadConceptos", new Document("$size", "$conceptosUnicos")))
        );

        List<Document> result = ejecutarPipeline(collection, pipeline);
        return result.isEmpty() ? new HashMap<>() : result.get(0);
    }

    /**
     * Método auxiliar para ejecutar pipelines
     */
    private List<Document> ejecutarPipeline(MongoCollection<Document> collection, List<Document> pipeline) {
        AggregateIterable<Document> result = collection.aggregate(pipeline);
        List<Document> resultados = new ArrayList<>();

        for (Document doc : result) {
            resultados.add(doc);
        }

        logger.info("Pipeline ejecutado, {} resultados obtenidos", resultados.size());
        return resultados;
    }

    // Agregar este método a IndicadoresGeneralesInfrastructureRepository.java

    /**
     * Obtiene indicadores con estadísticas calculadas y alertas
     */
    public List<Document> obtenerIndicadoresConAlertas(String periodoActual, String sucursal) {
        logger.info("Obteniendo indicadores con alertas para período: {}, sucursal: {}", periodoActual, sucursal);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        // Construir filtro de sucursal
        Document matchSucursal = new Document();
        if (sucursal != null && !sucursal.isEmpty()) {
            matchSucursal.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por sucursal si aplica
                new Document("$match", matchSucursal),

                // 2. Separar datos históricos vs actuales
                new Document("$facet", new Document()
                        // Calcular estadísticas SOLO con datos históricos (excluyendo período actual)
                        .append("estadisticasHistoricas", Arrays.asList(
                                new Document("$match", new Document("periodoActual", new Document("$ne", periodoActual))),
                                new Document("$group", new Document("_id", new Document()
                                        .append("puesto", "$fcDetalle5")
                                        .append("indicador", "$fcDetalle6")
                                        .append("sucursal", "$sucursal"))
                                        .append("variacionMedia", new Document("$avg", "$variacion"))
                                        .append("desviacionEstandar", new Document("$stdDevPop", "$variacion"))
                                        .append("cantidadPeriodos", new Document("$sum", 1)))
                        ))
                        // Obtener datos del período actual
                        .append("datosActuales", List.of(
                                new Document("$match", new Document("periodoActual", periodoActual))
                        ))
                ),

                // 3. Descomponer arrays
                new Document("$project", new Document()
                        .append("estadisticas", "$estadisticasHistoricas")
                        .append("actuales", "$datosActuales")),

                new Document("$unwind", "$actuales"),

                // 4. Buscar las estadísticas correspondientes
                new Document("$addFields", new Document("estadistica",
                        new Document("$arrayElemAt", Arrays.asList(
                                new Document("$filter", new Document()
                                        .append("input", "$estadisticas")
                                        .append("as", "e")
                                        .append("cond", new Document("$and", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$$e._id.puesto", "$actuales.fcDetalle5")),
                                                new Document("$eq", Arrays.asList("$$e._id.indicador", "$actuales.fcDetalle6")),
                                                new Document("$eq", Arrays.asList("$$e._id.sucursal", "$actuales.sucursal"))
                                        )))
                                ), 0
                        ))
                )),

                // 5. Calcular límites y z-score
                new Document("$project", new Document("_id", 0)
                        .append("puesto", "$actuales.fcDetalle5")
                        .append("indicador", "$actuales.fcDetalle6")
                        .append("sucursal", "$actuales.sucursal")
                        .append("negocio", "$actuales.negocio")
                        .append("conceptoDetalle", "$actuales.conceptoDetalle")
                        .append("periodoActual", "$actuales.periodoActual")
                        .append("valorActual", "$actuales.valorActual")
                        .append("periodoAnterior", "$actuales.periodoAnterior")
                        .append("valorAnterior", "$actuales.valorAnterior")
                        .append("diferencia", "$actuales.diferencia")
                        .append("variacion", "$actuales.variacion")
                        .append("variacionMedia", "$estadistica.variacionMedia")
                        .append("desviacionEstandar", "$estadistica.desviacionEstandar")
                        .append("cantidadPeriodos", "$estadistica.cantidadPeriodos")
                        // Límites: Media ± 1.96 × Desviación
                        .append("limiteInferior", new Document("$subtract", Arrays.asList(
                                "$estadistica.variacionMedia",
                                new Document("$multiply", Arrays.asList(1.96, "$estadistica.desviacionEstandar")))))
                        .append("limiteSuperior", new Document("$add", Arrays.asList(
                                "$estadistica.variacionMedia",
                                new Document("$multiply", Arrays.asList(1.96, "$estadistica.desviacionEstandar")))))
                        // Z-Score: |Variación Actual - Media| / Desviación
                        .append("zScore", new Document("$cond", Arrays.asList(
                                new Document("$or", Arrays.asList(
                                        new Document("$eq", Arrays.asList("$estadistica.desviacionEstandar", 0)),
                                        new Document("$eq", Arrays.asList("$estadistica.desviacionEstandar", null))
                                )),
                                0,
                                new Document("$abs", new Document("$divide", Arrays.asList(
                                        new Document("$subtract", Arrays.asList("$actuales.variacion", "$estadistica.variacionMedia")),
                                        "$estadistica.desviacionEstandar"
                                ))))))),

                // 6. Determinar severidad
                new Document("$addFields", new Document("severidad",
                        new Document("$switch", new Document()
                                .append("branches", Arrays.asList(
                                        new Document("case", new Document("$gte", Arrays.asList("$zScore", 2.5)))
                                                .append("then", "CRÍTICA"),
                                        new Document("case", new Document("$gte", Arrays.asList("$zScore", 1.96)))
                                                .append("then", "ALTA"),
                                        new Document("case", new Document("$gte", Arrays.asList("$zScore", 1.0)))
                                                .append("then", "MODERADA")
                                ))
                                .append("default", "NORMAL")))),

                // 7. Proyección final con redondeo
                new Document("$project", new Document("_id", 0)
                        .append("puesto", new Document("$trim", new Document("input", "$puesto")))
                        .append("indicador", new Document("$trim", new Document("input", "$indicador")))
                        .append("sucursal", new Document("$trim", new Document("input", "$sucursal")))
                        .append("negocio", "$negocio")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("periodoActual", "$periodoActual")
                        .append("valorActual", new Document("$round", Arrays.asList("$valorActual", 2)))
                        .append("periodoAnterior", "$periodoAnterior")
                        .append("valorAnterior", new Document("$round", Arrays.asList("$valorAnterior", 2)))
                        .append("diferenciaPesos", new Document("$round", Arrays.asList("$diferencia", 2)))
                        .append("variacionPorcentual", new Document("$round", Arrays.asList("$variacion", 2)))
                        .append("variacionMedia", new Document("$ifNull", Arrays.asList(
                                new Document("$round", Arrays.asList("$variacionMedia", 2)),
                                0)))
                        .append("limiteInferior", new Document("$ifNull", Arrays.asList(
                                new Document("$round", Arrays.asList("$limiteInferior", 2)),
                                0)))
                        .append("limiteSuperior", new Document("$ifNull", Arrays.asList(
                                new Document("$round", Arrays.asList("$limiteSuperior", 2)),
                                0)))
                        .append("zScore", new Document("$round", Arrays.asList("$zScore", 2)))
                        .append("severidad", "$severidad")
                        .append("cantidadPeriodos", new Document("$ifNull", Arrays.asList("$cantidadPeriodos", 0)))),

                // 8. Ordenar por severidad y z-score
                new Document("$sort", new Document()
                        .append("zScore", -1)
                        .append("puesto", 1))
        );

        return ejecutarPipeline(collection, pipeline);
    }

    /**
     * Obtiene estadísticas históricas por puesto e indicador (excluyendo período actual)
     */
    public List<Document> obtenerEstadisticasHistoricas(String periodoActual, String sucursal) {
        logger.info("Calculando estadísticas históricas para período: {}, sucursal: {}", periodoActual, sucursal);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        Document matchDocument = new Document();
        if (sucursal != null && !sucursal.isEmpty()) {
            matchDocument.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }
        // Excluir el período actual del cálculo de estadísticas
        if (periodoActual != null && !periodoActual.isEmpty()) {
            matchDocument.append("periodoActual", new Document("$ne", periodoActual));
        }

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar
                new Document("$match", matchDocument),

                // 2. Agrupar y calcular estadísticas
                new Document("$group", new Document("_id", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("sucursal", "$sucursal"))
                        .append("variacionMedia", new Document("$avg", "$variacion"))
                        .append("desviacionEstandar", new Document("$stdDevPop", "$variacion"))
                        .append("variacionMin", new Document("$min", "$variacion"))
                        .append("variacionMax", new Document("$max", "$variacion"))
                        .append("cantidadPeriodos", new Document("$sum", 1))),

                // 3. Calcular límites
                new Document("$project", new Document("_id", 0)
                        .append("puesto", new Document("$trim", new Document("input", "$_id.puesto")))
                        .append("indicador", new Document("$trim", new Document("input", "$_id.indicador")))
                        .append("sucursal", new Document("$trim", new Document("input", "$_id.sucursal")))
                        .append("variacionMedia", new Document("$round", Arrays.asList("$variacionMedia", 2)))
                        .append("desviacionEstandar", new Document("$round", Arrays.asList("$desviacionEstandar", 2)))
                        .append("limiteInferior", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList(
                                        "$variacionMedia",
                                        new Document("$multiply", Arrays.asList(1.96, "$desviacionEstandar"))
                                )), 2)))
                        .append("limiteSuperior", new Document("$round", Arrays.asList(
                                new Document("$add", Arrays.asList(
                                        "$variacionMedia",
                                        new Document("$multiply", Arrays.asList(1.96, "$desviacionEstandar"))
                                )), 2)))
                        .append("variacionMinima", new Document("$round", Arrays.asList("$variacionMin", 2)))
                        .append("variacionMaxima", new Document("$round", Arrays.asList("$variacionMax", 2)))
                        .append("cantidadPeriodos", "$cantidadPeriodos")),

                // 4. Ordenar
                new Document("$sort", new Document()
                        .append("puesto", 1)
                        .append("indicador", 1))
        );

        return ejecutarPipeline(collection, pipeline);
    }
}