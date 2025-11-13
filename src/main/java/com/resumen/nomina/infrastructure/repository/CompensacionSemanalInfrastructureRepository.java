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

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Repository
public class CompensacionSemanalInfrastructureRepository {

    private static final Logger logger = LoggerFactory.getLogger(CompensacionSemanalInfrastructureRepository.class);
    private final MongoTemplate mongoTemplate;

    @Autowired
    public CompensacionSemanalInfrastructureRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Pipeline para calcular compensación semanal nacional (semana actual vs anterior)
     * Devuelve resultado directo sin guardarlo
     */
    public Document calcularCompensacionSemanalNacional() {
        logger.info("Ejecutando pipeline de compensación semanal nacional");

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = createPipelineCompensacionSemanal();

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        for (Document doc : result) {
            logger.info("Resultado compensación semanal: {}", doc.toJson());
            return doc; // Devolver el primer (y único) resultado
        }

        return null; // No hay resultados
    }

    /**
     * Pipeline para obtener datos históricos de compensación (para gráfica)
     * Desde un año específico hacia adelante
     */
    public List<Document> obtenerDatosHistoricosCompensacion(Integer anioDesde) {
        logger.info("Ejecutando pipeline de datos históricos desde año: {}", anioDesde);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = createPipelineGraficaHistorica(anioDesde);

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        List<Document> resultados = new ArrayList<>();
        for (Document doc : result) {
            resultados.add(doc);
        }

        logger.info("Obtenidos {} puntos históricos para gráfica", resultados.size());
        return resultados;
    }

    /**
     * Calcular estadísticas históricas (media y desviación estándar) desde un año
     */
    public Document calcularEstadisticasHistoricas(Integer anioDesde) {
        logger.info("Calculando estadísticas históricas desde año: {}", anioDesde);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = createPipelineEstadisticasHistoricas(anioDesde);

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        for (Document doc : result) {
            logger.info("Estadísticas calculadas: {}", doc.toJson());
            return doc;
        }

        return null;
    }

    /**
     * Obtiene todas las semanas disponibles en IndicadoresCalculados
     */
    public List<String> obtenerSemanasDisponibles() {
        logger.info("Obteniendo todas las semanas disponibles en IndicadoresCalculados");

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar solo registros de compensación
                new Document("$match", new Document("conceptoDetalle", 1001)),

                // 2. Agrupar por período para obtener semanas únicas
                new Document("$group", new Document("_id", "$periodoActual")),

                // 3. Ordenar cronológicamente
                new Document("$sort", new Document("_id", 1)),

                // 4. Proyección simple
                new Document("$project", new Document("_id", 0).append("semana", "$_id"))
        );

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        List<String> semanas = new ArrayList<>();
        for (Document doc : result) {
            semanas.add(doc.getString("semana"));
        }

        logger.info("Se encontraron {} semanas únicas en IndicadoresCalculados", semanas.size());
        return semanas;
    }

    /**
     * Calcula compensación para una semana específica
     */
    public Document calcularCompensacionSemanaIndividual(String semana) {
        logger.info("Calculando compensación para semana específica: {}", semana);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar solo la semana específica y compensación
                new Document("$match", new Document()
                        .append("conceptoDetalle", 1001)
                        .append("periodoActual", semana)),

                // 2. Extraer información de la semana
                new Document("$addFields", new Document()
                        .append("semana", "$periodoActual")
                        .append("anio", new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 0, 4))))
                        .append("numeroSemana", new Document("$cond", new Document()
                                .append("if", new Document("$gt", Arrays.asList(
                                        new Document("$indexOfBytes", Arrays.asList("$periodoActual", "W")), -1)))
                                .append("then", new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 5, 2))))
                                .append("else", new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 4, 2))))))),

                // 3. Agrupar y sumar para esta semana específica
                new Document("$group", new Document("_id", new Document()
                        .append("semana", "$semana")
                        .append("anio", "$anio")
                        .append("numeroSemana", "$numeroSemana"))
                        .append("totalCompensacion", new Document("$sum", "$valorActual"))
                        .append("totalRegistros", new Document("$sum", 1))
                        .append("negocios", new Document("$addToSet", "$negocio"))
                        .append("puestos", new Document("$addToSet", "$puesto"))),

                // 4. Proyección final
                new Document("$project", new Document("_id", 0)
                        .append("semana", "$_id.semana")
                        .append("anio", "$_id.anio")
                        .append("numeroSemana", "$_id.numeroSemana")
                        .append("totalCompensacion", "$totalCompensacion")
                        .append("totalRegistros", "$totalRegistros")
                        .append("cantidadNegocios", new Document("$size", "$negocios"))
                        .append("cantidadPuestos", new Document("$size", "$puestos")))
        );

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        for (Document doc : result) {
            logger.info("Compensación calculada para semana {}: ${}", semana, doc.getDouble("totalCompensacion").longValue());
            return doc;
        }

        logger.warn("No se encontraron datos para la semana: {}", semana);
        return null;
    }

    // Pipeline para compensación semanal nacional (actual vs anterior)
    private List<Document> createPipelineCompensacionSemanal() {
        return Arrays.asList(
                // 1. Filtrar solo registros de compensación
                new Document("$match", new Document("conceptoDetalle", 1001)),

                // 2. Extraer información de semana
                new Document("$addFields", new Document()
                        .append("semana", "$periodoActual")
                        .append("anio", new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 0, 4))))
                        .append("numeroSemana", new Document("$cond", new Document()
                                .append("if", new Document("$gt", Arrays.asList(
                                        new Document("$indexOfBytes", Arrays.asList("$periodoActual", "W")), -1)))
                                .append("then", new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 5, 2))))
                                .append("else", new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 4, 2))))))),

                // 3. Agrupar por semana y sumar compensación nacional
                new Document("$group", new Document("_id", new Document()
                        .append("semana", "$semana")
                        .append("anio", "$anio")
                        .append("numeroSemana", "$numeroSemana"))
                        .append("totalCompensacion", new Document("$sum", "$valorActual"))
                        .append("totalRegistros", new Document("$sum", 1))
                        .append("negocios", new Document("$addToSet", "$negocio"))
                        .append("puestos", new Document("$addToSet", "$puesto"))),

                // 4. Ordenar cronológicamente
                new Document("$sort", new Document()
                        .append("_id.anio", 1)
                        .append("_id.numeroSemana", 1)),

                // 5. Agrupar todo para obtener array ordenado
                new Document("$group", new Document("_id", null)
                        .append("semanas", new Document("$push", new Document()
                                .append("semana", "$_id.semana")
                                .append("anio", "$_id.anio")
                                .append("numeroSemana", "$_id.numeroSemana")
                                .append("totalCompensacion", "$totalCompensacion")
                                .append("totalRegistros", "$totalRegistros")
                                .append("cantidadNegocios", new Document("$size", "$negocios"))
                                .append("cantidadPuestos", new Document("$size", "$puestos"))))),

                // 6. Proyectar para obtener últimas 2 semanas
                new Document("$project", new Document("_id", 0)
                        .append("semanaActual", new Document("$arrayElemAt", Arrays.asList("$semanas", -1)))
                        .append("semanaAnterior", new Document("$arrayElemAt", Arrays.asList("$semanas", -2)))
                        .append("totalSemanas", new Document("$size", "$semanas"))),

                // 7. Calcular diferencia y variación
                new Document("$addFields", new Document()
                        .append("compensacionSemanaActual", "$semanaActual.totalCompensacion")
                        .append("compensacionSemanaAnterior", "$semanaAnterior.totalCompensacion")
                        .append("diferenciaPesos", new Document("$subtract", Arrays.asList(
                                "$semanaActual.totalCompensacion",
                                "$semanaAnterior.totalCompensacion")))
                        .append("variacionPorcentual", new Document("$cond", new Document()
                                .append("if", new Document("$eq", Arrays.asList("$semanaAnterior.totalCompensacion", 0)))
                                .append("then", null)
                                .append("else", new Document("$multiply", Arrays.asList(
                                        new Document("$subtract", Arrays.asList(
                                                new Document("$divide", Arrays.asList(
                                                        "$semanaActual.totalCompensacion",
                                                        "$semanaAnterior.totalCompensacion")), 1)), 100)))))),

                // 8. Proyección final simplificada
                new Document("$project", new Document("_id", 0)
                        .append("semanaActual", "$semanaActual.semana")
                        .append("semanaAnterior", "$semanaAnterior.semana")
                        .append("totalSemanaActual", new Document("$round", Arrays.asList("$compensacionSemanaActual", 0)))
                        .append("totalSemanaAnterior", new Document("$round", Arrays.asList("$compensacionSemanaAnterior", 0)))
                        .append("diferenciaPesos", new Document("$round", Arrays.asList("$diferenciaPesos", 0)))
                        .append("variacionPorcentual", new Document("$round", Arrays.asList("$variacionPorcentual", 1)))
                        .append("datosActual", "$semanaActual")
                        .append("datosAnterior", "$semanaAnterior"))
        );
    }

    // Pipeline para gráfica histórica desde un año específico
    private List<Document> createPipelineGraficaHistorica(Integer anioDesde) {
        return Arrays.asList(
                // 1. Filtrar compensación desde el año especificado
                new Document("$match", new Document()
                        .append("conceptoDetalle", 1001)
                        .append("$expr", new Document("$gte", Arrays.asList(
                                new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 0, 4))),
                                anioDesde)))),

                // 2. Agrupar por semana y sumar compensación
                new Document("$group", new Document("_id", "$periodoActual")
                        .append("totalCompensacion", new Document("$sum", "$valorActual"))
                        .append("cantidadRegistros", new Document("$sum", 1))),

                // 3. Ordenar cronológicamente
                new Document("$sort", new Document("_id", 1)),

                // 4. Proyección final con estructura para gráfica
                new Document("$project", new Document("_id", 0)
                        .append("semana", "$_id")
                        .append("total", new Document("$round", Arrays.asList("$totalCompensacion", 0)))
                        .append("registros", "$cantidadRegistros"))
        );
    }

    // Pipeline para calcular estadísticas históricas
    private List<Document> createPipelineEstadisticasHistoricas(Integer anioDesde) {
        return Arrays.asList(
                // 1. Filtrar compensación desde el año especificado
                new Document("$match", new Document()
                        .append("conceptoDetalle", 1001)
                        .append("$expr", new Document("$gte", Arrays.asList(
                                new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 0, 4))),
                                anioDesde)))),

                // 2. Agrupar por semana y sumar
                new Document("$group", new Document("_id", "$periodoActual")
                        .append("totalCompensacion", new Document("$sum", "$valorActual"))),

                // 3. Calcular estadísticas generales
                new Document("$group", new Document("_id", null)
                        .append("totalSemanas", new Document("$sum", 1))
                        .append("sumaTotal", new Document("$sum", "$totalCompensacion"))
                        .append("sumaCuadrados", new Document("$sum",
                                new Document("$multiply", Arrays.asList("$totalCompensacion", "$totalCompensacion"))))),

                // 4. Calcular media y desviación estándar
                new Document("$addFields", new Document()
                        .append("media", new Document("$divide", Arrays.asList("$sumaTotal", "$totalSemanas")))
                        .append("desviacionEstandar", new Document("$sqrt", new Document("$subtract", Arrays.asList(
                                new Document("$divide", Arrays.asList("$sumaCuadrados", "$totalSemanas")),
                                new Document("$multiply", Arrays.asList(
                                        new Document("$divide", Arrays.asList("$sumaTotal", "$totalSemanas")),
                                        new Document("$divide", Arrays.asList("$sumaTotal", "$totalSemanas"))))))))),

                // 5. Proyección final con líneas de confianza
                new Document("$project", new Document("_id", 0)
                        .append("media", new Document("$round", Arrays.asList("$media", 0)))
                        .append("desviacionEstandar", new Document("$round", Arrays.asList("$desviacionEstandar", 0)))
                        .append("totalSemanas", "$totalSemanas")
                        .append("superior1DS", new Document("$round", Arrays.asList(
                                new Document("$add", Arrays.asList("$media", "$desviacionEstandar")), 0)))
                        .append("inferior1DS", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList("$media", "$desviacionEstandar")), 0)))
                        .append("superior15DS", new Document("$round", Arrays.asList(
                                new Document("$add", Arrays.asList("$media",
                                        new Document("$multiply", Arrays.asList("$desviacionEstandar", 1.5)))), 0)))
                        .append("inferior15DS", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList("$media",
                                        new Document("$multiply", Arrays.asList("$desviacionEstandar", 1.5)))), 0))))
        );
    }


    /**
     * Pipeline para obtener datos históricos de compensación filtrado por negocio específico
     */
    public List<Document> obtenerDatosHistoricosCompensacionPorNegocio(Integer negocio, Integer anioDesde) {
        logger.info("Ejecutando pipeline de datos históricos para negocio: {} desde año: {}", negocio, anioDesde);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = createPipelineGraficaHistoricaPorNegocio(negocio, anioDesde);

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        List<Document> resultados = new ArrayList<>();
        for (Document doc : result) {
            resultados.add(doc);
        }

        logger.info("Obtenidos {} puntos históricos para gráfica del negocio {}", resultados.size(), negocio);
        return resultados;
    }

    /**
     * Calcular estadísticas históricas para un negocio específico desde un año
     */
    public Document calcularEstadisticasHistoricasPorNegocio(Integer negocio, Integer anioDesde) {
        logger.info("Calculando estadísticas históricas para negocio: {} desde año: {}", negocio, anioDesde);

        MongoDatabase database = mongoTemplate.getDb();
        MongoCollection<Document> collection = database.getCollection("IndicadoresCalculados");

        List<Document> pipeline = createPipelineEstadisticasHistoricasPorNegocio(negocio, anioDesde);

        AggregateIterable<Document> result = collection.aggregate(pipeline);

        for (Document doc : result) {
            logger.info("Estadísticas calculadas para negocio {}: {}", negocio, doc.toJson());
            return doc;
        }

        return null;
    }

    // Pipeline para gráfica histórica de un negocio específico
    private List<Document> createPipelineGraficaHistoricaPorNegocio(Integer negocio, Integer anioDesde) {
        return Arrays.asList(
                // 1. Filtrar compensación del negocio específico desde el año especificado
                new Document("$match", new Document()
                        .append("conceptoDetalle", 1001)
                        .append("negocio", negocio)  // <- FILTRO POR NEGOCIO
                        .append("$expr", new Document("$gte", Arrays.asList(
                                new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 0, 4))),
                                anioDesde)))),

                // 2. Agrupar por semana y sumar compensación
                new Document("$group", new Document("_id", "$periodoActual")
                        .append("totalCompensacion", new Document("$sum", "$valorActual"))
                        .append("cantidadRegistros", new Document("$sum", 1))),

                // 3. Ordenar cronológicamente
                new Document("$sort", new Document("_id", 1)),

                // 4. Proyección final con estructura para gráfica
                new Document("$project", new Document("_id", 0)
                        .append("semana", "$_id")
                        .append("total", new Document("$round", Arrays.asList("$totalCompensacion", 0)))
                        .append("registros", "$cantidadRegistros"))
        );
    }

    // Pipeline para estadísticas históricas de un negocio específico
    private List<Document> createPipelineEstadisticasHistoricasPorNegocio(Integer negocio, Integer anioDesde) {
        return Arrays.asList(
                // 1. Filtrar compensación del negocio específico desde el año especificado
                new Document("$match", new Document()
                        .append("conceptoDetalle", 1001)
                        .append("negocio", negocio)  // <- FILTRO POR NEGOCIO
                        .append("$expr", new Document("$gte", Arrays.asList(
                                new Document("$toInt", new Document("$substr", Arrays.asList("$periodoActual", 0, 4))),
                                anioDesde)))),

                // 2. Agrupar por semana y sumar
                new Document("$group", new Document("_id", "$periodoActual")
                        .append("totalCompensacion", new Document("$sum", "$valorActual"))),

                // 3. Calcular estadísticas generales
                new Document("$group", new Document("_id", null)
                        .append("totalSemanas", new Document("$sum", 1))
                        .append("sumaTotal", new Document("$sum", "$totalCompensacion"))
                        .append("sumaCuadrados", new Document("$sum",
                                new Document("$multiply", Arrays.asList("$totalCompensacion", "$totalCompensacion"))))),

                // 4. Calcular media y desviación estándar
                new Document("$addFields", new Document()
                        .append("media", new Document("$divide", Arrays.asList("$sumaTotal", "$totalSemanas")))
                        .append("desviacionEstandar", new Document("$sqrt", new Document("$subtract", Arrays.asList(
                                new Document("$divide", Arrays.asList("$sumaCuadrados", "$totalSemanas")),
                                new Document("$multiply", Arrays.asList(
                                        new Document("$divide", Arrays.asList("$sumaTotal", "$totalSemanas")),
                                        new Document("$divide", Arrays.asList("$sumaTotal", "$totalSemanas"))))))))),

                // 5. Proyección final con líneas de confianza
                new Document("$project", new Document("_id", 0)
                        .append("media", new Document("$round", Arrays.asList("$media", 0)))
                        .append("desviacionEstandar", new Document("$round", Arrays.asList("$desviacionEstandar", 0)))
                        .append("totalSemanas", "$totalSemanas")
                        .append("superior1DS", new Document("$round", Arrays.asList(
                                new Document("$add", Arrays.asList("$media", "$desviacionEstandar")), 0)))
                        .append("inferior1DS", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList("$media", "$desviacionEstandar")), 0)))
                        .append("superior15DS", new Document("$round", Arrays.asList(
                                new Document("$add", Arrays.asList("$media",
                                        new Document("$multiply", Arrays.asList("$desviacionEstandar", 1.5)))), 0)))
                        .append("inferior15DS", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList("$media",
                                        new Document("$multiply", Arrays.asList("$desviacionEstandar", 1.5)))), 0))))
        );
    }

}