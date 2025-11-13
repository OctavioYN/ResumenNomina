package com.resumen.nomina.infrastructure.repository;

import com.mongodb.client.MongoCollection;
import com.resumen.nomina.domain.model.ConfiguracionAlertas;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AlertasInfrastructureRepository {

    private static final Logger log = LoggerFactory.getLogger(AlertasInfrastructureRepository.class);
    private final MongoTemplate mongoTemplate;

    @Autowired
    public AlertasInfrastructureRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * SISTEMA 1: Z-Score con configuración dinámica
     */
    public List<Document> calcularAlertasZScore(String periodoActual, String sucursal,
                                                Integer negocio, ConfiguracionAlertas config) {
        log.info("Calculando alertas Z-Score con configuración: periodosMin={}, umbralCritico={}",
                config.getPeriodosMinimosHistoricos(), config.getUmbralCritico());

        MongoCollection<Document> collection = mongoTemplate.getDb()
                .getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                new Document("$facet", new Document()
                        .append("estadisticasHistoricas", Arrays.asList(
                                new Document("$match", buildMatchFilter(periodoActual, sucursal, negocio, config, true)),
                                new Document("$group", new Document("_id", new Document()
                                        .append("puesto", "$fcDetalle5")
                                        .append("indicador", "$fcDetalle6")
                                        .append("conceptoDetalle", "$conceptoDetalle")
                                        .append("sucursal", "$sucursal")
                                        .append("negocio", "$negocio"))
                                        .append("variacionMedia", new Document("$avg", "$variacion"))
                                        .append("desviacionEstandar", new Document("$stdDevPop", "$variacion"))
                                        .append("cantidadPeriodos", new Document("$sum", 1)))
                        ))
                        .append("datosActuales", Arrays.asList(
                                new Document("$match", buildMatchFilter(periodoActual, sucursal, negocio, config, false)),
                                new Document("$match", new Document("periodoActual", periodoActual))
                        ))
                ),

                new Document("$project", new Document()
                        .append("estadisticas", "$estadisticasHistoricas")
                        .append("actuales", "$datosActuales")),

                new Document("$unwind", "$actuales"),

                new Document("$addFields", new Document("estadistica",
                        new Document("$arrayElemAt", Arrays.asList(
                                new Document("$filter", new Document()
                                        .append("input", "$estadisticas")
                                        .append("as", "e")
                                        .append("cond", new Document("$and", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$$e._id.puesto", "$actuales.fcDetalle5")),
                                                new Document("$eq", Arrays.asList("$$e._id.indicador", "$actuales.fcDetalle6")),
                                                new Document("$eq", Arrays.asList("$$e._id.conceptoDetalle", "$actuales.conceptoDetalle")),
                                                new Document("$eq", Arrays.asList("$$e._id.sucursal", "$actuales.sucursal")),
                                                new Document("$eq", Arrays.asList("$$e._id.negocio", "$actuales.negocio"))
                                        )))
                                ), 0
                        ))
                )),

                new Document("$project", new Document("_id", 0)
                        .append("puesto", new Document("$trim", new Document("input", "$actuales.fcDetalle5")))
                        .append("indicador", new Document("$trim", new Document("input", "$actuales.fcDetalle6")))
                        .append("conceptoDetalle", "$actuales.conceptoDetalle")
                        .append("sucursal", new Document("$trim", new Document("input", "$actuales.sucursal")))
                        .append("negocio", "$actuales.negocio")
                        .append("periodoActual", "$actuales.periodoActual")
                        .append("variacionVsSA", new Document("$toDouble",
                                new Document("$round", Arrays.asList("$actuales.variacion", 2))))
                        .append("variacionMedia", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$ifNull", Arrays.asList("$estadistica.variacionMedia", 0)), 2))))
                        .append("desviacionEstandar", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$ifNull", Arrays.asList("$estadistica.desviacionEstandar", 0)), 2))))

                        // CAMBIO: Usar nivel de confianza de la configuración
                        .append("limiteInferior", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$subtract", Arrays.asList(
                                                "$estadistica.variacionMedia",
                                                new Document("$multiply", Arrays.asList(
                                                        config.getNivelConfianzaArima(),
                                                        "$estadistica.desviacionEstandar"))
                                        )), 2))))
                        .append("limiteSuperior", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$add", Arrays.asList(
                                                "$estadistica.variacionMedia",
                                                new Document("$multiply", Arrays.asList(
                                                        config.getNivelConfianzaArima(),
                                                        "$estadistica.desviacionEstandar"))
                                        )), 2))))

                        .append("zScore", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$cond", Arrays.asList(
                                                new Document("$or", Arrays.asList(
                                                        new Document("$eq", Arrays.asList("$estadistica.desviacionEstandar", 0)),
                                                        new Document("$eq", Arrays.asList("$estadistica.desviacionEstandar", null))
                                                )),
                                                0,
                                                new Document("$divide", Arrays.asList(
                                                        new Document("$subtract", Arrays.asList(
                                                                "$actuales.variacion",
                                                                "$estadistica.variacionMedia"
                                                        )),
                                                        "$estadistica.desviacionEstandar"
                                                ))
                                        )), 2))))
                        .append("cantidadPeriodosHistoricos",
                                new Document("$ifNull", Arrays.asList("$estadistica.cantidadPeriodos", 0)))
                ),

                new Document("$addFields", new Document()
                        .append("zScoreAbs", new Document("$abs", "$zScore"))
                        .append("fueraDeRango", new Document("$or", Arrays.asList(
                                new Document("$lt", Arrays.asList("$variacionVsSA", "$limiteInferior")),
                                new Document("$gt", Arrays.asList("$variacionVsSA", "$limiteSuperior"))
                        )))
                ),

                // CAMBIO: Usar umbrales de la configuración
                new Document("$addFields", new Document("severidad",
                        new Document("$switch", new Document()
                                .append("branches", Arrays.asList(
                                        new Document("case", new Document("$gte", Arrays.asList("$zScoreAbs", config.getUmbralCritico())))
                                                .append("then", "CRITICA"),
                                        new Document("case", new Document("$gte", Arrays.asList("$zScoreAbs", config.getUmbralAlto())))
                                                .append("then", "ALTA"),
                                        new Document("case", new Document("$gte", Arrays.asList("$zScoreAbs", config.getUmbralModerado())))
                                                .append("then", "MODERADA")
                                ))
                                .append("default", "NORMAL"))
                )),

                // CAMBIO: Filtrar por períodos mínimos de la configuración
                new Document("$match", new Document("cantidadPeriodosHistoricos",
                        new Document("$gte", config.getPeriodosMinimosHistoricos()))),

                new Document("$sort", new Document()
                        .append("zScoreAbs", -1)
                        .append("puesto", 1))
        );

        return ejecutarPipeline(collection, pipeline);
    }

    /**
     * SISTEMA 2: ARIMA con configuración dinámica
     */
    public List<Document> calcularAlertasARIMA(String periodoActual, String sucursal,
                                               Integer negocio, ConfiguracionAlertas config) {
        log.info("Calculando alertas ARIMA con configuración: nivelConfianza={}, periodosRobusto={}",
                config.getNivelConfianzaArima(), config.getPeriodosModeloRobusto());

        MongoCollection<Document> collection = mongoTemplate.getDb()
                .getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                new Document("$match", buildMatchFilter(periodoActual, sucursal, negocio, config, false)),

                new Document("$group", new Document("_id", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("indicador", "$fcDetalle6")
                        .append("conceptoDetalle", "$conceptoDetalle")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio"))
                        .append("valores", new Document("$push", new Document()
                                .append("periodo", "$periodoActual")
                                .append("valor", new Document("$toDouble", "$valorActual"))))
                        .append("cantidadPeriodos", new Document("$sum", 1))
                ),

                new Document("$addFields", new Document("valoresOrdenados",
                        new Document("$sortArray", new Document()
                                .append("input", "$valores")
                                .append("sortBy", new Document("periodo", 1)))
                )),

                new Document("$addFields", new Document()
                        .append("valorActual", new Document("$last", "$valoresOrdenados"))
                        .append("tamanoArray", new Document("$size", "$valoresOrdenados"))
                ),

                new Document("$addFields", new Document("valoresHistoricos",
                        new Document("$slice", Arrays.asList(
                                "$valoresOrdenados",
                                0,
                                new Document("$subtract", Arrays.asList("$tamanoArray", 1))
                        ))
                )),

                new Document("$addFields", new Document()
                        .append("mediaHistorica", new Document("$avg", "$valoresHistoricos.valor"))
                        .append("desviacionHistorica", new Document("$stdDevPop", "$valoresHistoricos.valor"))
                ),

                new Document("$addFields", new Document()
                        .append("factorAjuste", new Document("$sqrt",
                                new Document("$add", Arrays.asList(
                                        1,
                                        new Document("$divide", Arrays.asList(1, "$cantidadPeriodos"))
                                ))
                        ))
                ),

                // CAMBIO: Usar nivel de confianza de la configuración
                new Document("$addFields", new Document()
                        .append("rangoPrediccion", new Document("$multiply", Arrays.asList(
                                config.getNivelConfianzaArima(),
                                "$desviacionHistorica",
                                "$factorAjuste"
                        )))
                ),

                new Document("$project", new Document("_id", 0)
                        .append("puesto", new Document("$trim", new Document("input", "$_id.puesto")))
                        .append("indicador", new Document("$trim", new Document("input", "$_id.indicador")))
                        .append("conceptoDetalle", "$_id.conceptoDetalle")
                        .append("sucursal", new Document("$trim", new Document("input", "$_id.sucursal")))
                        .append("negocio", "$_id.negocio")
                        .append("periodoActual", "$valorActual.periodo")
                        .append("observacionReal", new Document("$toDouble",
                                new Document("$round", Arrays.asList("$valorActual.valor", 2))))
                        .append("limiteInferior", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$subtract", Arrays.asList("$mediaHistorica", "$rangoPrediccion")), 2))))
                        .append("limiteSuperior", new Document("$toDouble",
                                new Document("$round", Arrays.asList(
                                        new Document("$add", Arrays.asList("$mediaHistorica", "$rangoPrediccion")), 2))))
                        .append("rangoPrediccion", new Document("$toDouble",
                                new Document("$round", Arrays.asList("$rangoPrediccion", 2))))
                        .append("fueraDeRango", new Document("$or", Arrays.asList(
                                new Document("$lt", Arrays.asList("$valorActual.valor",
                                        new Document("$subtract", Arrays.asList("$mediaHistorica", "$rangoPrediccion")))),
                                new Document("$gt", Arrays.asList("$valorActual.valor",
                                        new Document("$add", Arrays.asList("$mediaHistorica", "$rangoPrediccion"))))
                        )))
                        .append("cantidadPeriodosHistoricos", "$cantidadPeriodos")

                        // CAMBIO: Usar períodos robusto de la configuración
                        .append("modeloRobusto", new Document("$gte", Arrays.asList(
                                "$cantidadPeriodos", config.getPeriodosModeloRobusto())))
                ),

                new Document("$addFields", new Document()
                        .append("variacionFueraDelRango", new Document("$toDouble",
                                new Document("$cond", Arrays.asList(
                                        "$fueraDeRango",
                                        new Document("$round", Arrays.asList(
                                                new Document("$multiply", Arrays.asList(
                                                        new Document("$cond", Arrays.asList(
                                                                new Document("$gt", Arrays.asList("$observacionReal", "$limiteSuperior")),
                                                                new Document("$divide", Arrays.asList(
                                                                        new Document("$subtract", Arrays.asList("$observacionReal", "$limiteSuperior")),
                                                                        "$limiteSuperior"
                                                                )),
                                                                new Document("$divide", Arrays.asList(
                                                                        new Document("$subtract", Arrays.asList("$limiteInferior", "$observacionReal")),
                                                                        "$limiteInferior"
                                                                ))
                                                        )),
                                                        100
                                                )), 0
                                        )),
                                        0
                                ))))
                        .append("direccionDesviacion", new Document("$cond", Arrays.asList(
                                "$fueraDeRango",
                                new Document("$cond", Arrays.asList(
                                        new Document("$gt", Arrays.asList("$observacionReal", "$limiteSuperior")),
                                        "SUPERIOR",
                                        "INFERIOR"
                                )),
                                "NORMAL"
                        )))
                ),

                // CAMBIO: Filtrar por períodos mínimos de la configuración
                new Document("$match", new Document("cantidadPeriodosHistoricos",
                        new Document("$gte", config.getPeriodosMinimosHistoricos()))),

                new Document("$sort", new Document()
                        .append("variacionFueraDelRango", -1)
                        .append("puesto", 1))
        );

        return ejecutarPipeline(collection, pipeline);
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * CAMBIO: Construye filtro usando configuración para exclusiones
     */
    private Document buildMatchFilter(String periodoActual, String sucursal, Integer negocio,
                                      ConfiguracionAlertas config, boolean excluirActual) {
        Document match = new Document();

        if (excluirActual) {
            match.append("periodoActual", new Document("$ne", periodoActual));
        }

        if (sucursal != null && !sucursal.trim().isEmpty() && !"TODAS".equalsIgnoreCase(sucursal)) {
            match.append("sucursal", new Document("$regex", ".*" + sucursal.trim() + ".*")
                    .append("$options", "i"));
        }

        if (negocio != null && negocio > 0) {
            match.append("negocio", negocio);
        }

        // CAMBIO: Usar conceptos excluidos de la configuración
        if (config.getConceptosExcluidos() != null && !config.getConceptosExcluidos().isEmpty()) {
            match.append("conceptoDetalle", new Document("$nin", config.getConceptosExcluidos()));
        }

        // NUEVO: Excluir negocios si está configurado
        if (config.getNegociosExcluidos() != null && !config.getNegociosExcluidos().isEmpty()) {
            match.append("negocio", new Document("$nin", config.getNegociosExcluidos()));
        }

        // NUEVO: Excluir puestos si está configurado
        if (config.getPuestosExcluidos() != null && !config.getPuestosExcluidos().isEmpty()) {
            match.append("puesto", new Document("$nin", config.getPuestosExcluidos()));
        }

        return match;
    }

    private List<Document> ejecutarPipeline(MongoCollection<Document> collection, List<Document> pipeline) {
        try {
            List<Document> results = collection.aggregate(pipeline).into(new ArrayList<>());
            log.info("Pipeline ejecutado exitosamente. Resultados: {}", results.size());
            return results;
        } catch (Exception e) {
            log.error("Error ejecutando pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("Error en agregación MongoDB: " + e.getMessage(), e);
        }
    }
}