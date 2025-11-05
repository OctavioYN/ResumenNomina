/*package com.resumen.nomina.infrastructure.repository;

import com.resumen.nomina.domain.model.Indicador;
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

@Repository
public class CalculoIndicadorRepository implements com.resumen.nomina.application.repository.CalculoIndicadorRepository {

    private static final Logger logger = LoggerFactory.getLogger(CalculoIndicadorRepository.class);

    private final MongoTemplate mongoTemplate;

    @Autowired
    public CalculoIndicadorRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Indicador> obtenerIndicadoresPorPeriodos(List<String> periodos) {
        logger.info("Ejecutando el pipeline de agregación para los periodos: {}", periodos);

        // Obtiene la base de datos de MongoDB
        MongoDatabase database = mongoTemplate.getDb();

        // Especifica la colección 'DatosInteligencia' donde realizar el pipeline
        MongoCollection<Document> collection = database.getCollection("DatosInteligencia");

        // Define el pipeline de agregación
        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("PkiPeriodo", new Document("$in", periodos))),

                new Document("$group", new Document("_id", new Document("puesto", "$PkiPuesto")
                        .append("sucursal", "$FcDetalle4")
                        .append("FcDetalle5", "$FcDetalle5")
                        .append("FcDetalle6", "$FcDetalle6")
                        .append("negocio", "$PkiGrupoNegocio")
                        .append("conceptoDetalle", "$PkiConceptoDetalle")
                        .append("periodo", "$PkiPeriodo"))
                        .append("valor", new Document("$sum", "$FnValor"))),

                new Document("$group", new Document("_id", new Document("puesto", "$_id.puesto")
                        .append("sucursal", "$_id.sucursal")
                        .append("FcDetalle5", "$_id.FcDetalle5")
                        .append("FcDetalle6", "$_id.FcDetalle6")
                        .append("negocio", "$_id.negocio")
                        .append("conceptoDetalle", "$_id.conceptoDetalle"))
                        .append("valores", new Document("$push", new Document("periodo", "$_id.periodo")
                                .append("valor", "$valor")))),

                // 1. Desenrollar los valores para trabajar con los periodos y sus valores correspondientes
                new Document("$unwind", "$valores"),

                // 2. Ordenar los periodos de menor a mayor
                new Document("$sort", new Document("valores.periodo", 1)),

                // 3. Volver a agrupar para que los valores ordenados se mantengan
                new Document("$group", new Document("_id", new Document("puesto", "$_id.puesto")
                        .append("sucursal", "$_id.sucursal")
                        .append("FcDetalle5", "$_id.FcDetalle5")
                        .append("FcDetalle6", "$_id.FcDetalle6")
                        .append("negocio", "$_id.negocio")
                        .append("conceptoDetalle", "$_id.conceptoDetalle"))
                        .append("valores", new Document("$push", "$valores"))),

                // 4. Proyección de los periodos y cálculos de diferencia y variación
                new Document("$project", new Document("puesto", "$_id.puesto")
                        .append("sucursal", "$_id.sucursal")
                        .append("FcDetalle5", "$_id.FcDetalle5")
                        .append("FcDetalle6", "$_id.FcDetalle6")
                        .append("negocio", "$_id.negocio")
                        .append("conceptoDetalle", "$_id.conceptoDetalle")

                        // Definir el periodo anterior como el menor
                        .append("PeriodoAnterior", new Document("$arrayElemAt", Arrays.asList("$valores.periodo", 0)))

                        // Valor del periodo anterior
                        .append("valorAnterior", new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0)))

                        // Definir el periodo actual como el mayor
                        .append("PeriodoActual", new Document("$arrayElemAt", Arrays.asList("$valores.periodo", 1)))

                        // Valor del periodo actual
                        .append("valorActual", new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)))

                        // Diferencia entre el valor actual y el valor anterior
                        .append("diferencia", new Document("$subtract", Arrays.asList(
                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)),
                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0))
                        )))

                        // Variación entre el valor actual y el valor anterior
                        .append("variacion", new Document("$cond", new Document("if",
                                new Document("$eq", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0)), 0)))
                                .append("then", 0)
                                .append("else", new Document("$multiply", Arrays.asList(
                                        new Document("$divide", Arrays.asList(
                                                new Document("$subtract", Arrays.asList(
                                                        new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)),
                                                        new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0))
                                                )),
                                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0))
                                        )),
                                        100
                                )))))));

        // Ejecutar el pipeline
        logger.info("Ejecutando la agregación en la colección 'DatosInteligencia'...");
        AggregateIterable<Document> result = collection.aggregate(pipeline);

        logger.info("Agregación completada, procesando los resultados...");

        // Mapear los resultados a una lista de objetos Indicador
        return result.map(document -> {
            Indicador indicador = new Indicador();
            indicador.setPuesto(document.getInteger("puesto"));
            indicador.setSucursal(document.getString("sucursal"));
            indicador.setFcDetalle5(document.getString("FcDetalle5"));
            indicador.setFcDetalle6(document.getString("FcDetalle6"));
            indicador.setNegocio(document.getInteger("negocio"));
            indicador.setConceptoDetalle(document.getInteger("conceptoDetalle"));
            indicador.setPeriodoAnterior(document.getString("PeriodoAnterior"));
            indicador.setValorAnterior(getDoubleValue(document, "valorAnterior"));
            indicador.setPeriodoActual(document.getString("PeriodoActual"));
            indicador.setValorActual(getDoubleValue(document, "valorActual"));
            indicador.setDiferencia(getDoubleValue(document, "diferencia"));
            indicador.setVariacion(getDoubleValue(document, "variacion"));
            return indicador;
        }).into(new java.util.ArrayList<>());
    }

    // Método auxiliar para convertir cualquier valor numérico a Double
    private Double getDoubleValue(Document document, String field) {
        Object value = document.get(field);
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else {
            return 0.0;  // Valor por defecto si no es un número
        }
    }
}
*/
package com.resumen.nomina.infrastructure.repository;

import com.resumen.nomina.domain.model.Indicador;
import com.resumen.nomina.domain.model.IndicadorCalculado;
import com.resumen.nomina.application.repository.IndicadorCalculadoRepository;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Repository
public class CalculoIndicadorRepository implements com.resumen.nomina.application.repository.CalculoIndicadorRepository {

    private static final Logger logger = LoggerFactory.getLogger(CalculoIndicadorRepository.class);

    private final MongoTemplate mongoTemplate;
    private final IndicadorCalculadoRepository indicadorCalculadoRepository;

    @Autowired
    public CalculoIndicadorRepository(MongoTemplate mongoTemplate, IndicadorCalculadoRepository indicadorCalculadoRepository) {
        this.mongoTemplate = mongoTemplate;
        this.indicadorCalculadoRepository = indicadorCalculadoRepository;
    }

    @Override
    public List<Indicador> obtenerIndicadoresPorPeriodos(List<String> periodos) {
        logger.info("Ejecutando el pipeline de agregación para los periodos: {}", periodos);

        // Obtiene la base de datos de MongoDB
        MongoDatabase database = mongoTemplate.getDb();

        // Especifica la colección 'DatosInteligencia' donde realizar el pipeline
        MongoCollection<Document> collection = database.getCollection("DatosInteligencia");

        // Define el pipeline de agregación
        List<Document> pipeline = createAggregationPipeline(periodos);

        // Ejecutar el pipeline
        logger.info("Ejecutando la agregación en la colección 'DatosInteligencia'...");
        AggregateIterable<Document> result = collection.aggregate(pipeline);

        logger.info("Agregación completada, procesando los resultados...");

        // Mapear los resultados a una lista de objetos Indicador
        return result.map(document -> {
            Indicador indicador = new Indicador();
            indicador.setPuesto(document.getInteger("puesto"));
            indicador.setSucursal(document.getString("sucursal"));
            indicador.setFcDetalle5(document.getString("FcDetalle5"));
            indicador.setFcDetalle6(document.getString("FcDetalle6"));
            indicador.setNegocio(document.getInteger("negocio"));
            indicador.setConceptoDetalle(document.getInteger("conceptoDetalle"));
            indicador.setPeriodoAnterior(document.getString("PeriodoAnterior"));
            indicador.setValorAnterior(getDoubleValue(document, "valorAnterior"));
            indicador.setPeriodoActual(document.getString("PeriodoActual"));
            indicador.setValorActual(getDoubleValue(document, "valorActual"));
            indicador.setDiferencia(getDoubleValue(document, "diferencia"));
            indicador.setVariacion(getDoubleValue(document, "variacion"));
            return indicador;
        }).into(new java.util.ArrayList<>());
    }

    /**
     * Calcula y guarda los indicadores en la colección IndicadoresCalculados
     */
    @Transactional
    public List<IndicadorCalculado> calcularYGuardarIndicadores(List<String> periodos, String usuario) {
        logger.info("Iniciando cálculo y guardado para periodos: {} por usuario: {}", periodos, usuario);

        try {
            // 1. Ejecutar el cálculo
            List<Indicador> indicadoresCalculados = obtenerIndicadoresPorPeriodos(periodos);
            logger.info("Se calcularon {} indicadores", indicadoresCalculados.size());

            // 2. Convertir a IndicadorCalculado y guardar
            List<IndicadorCalculado> indicadoresParaGuardar = new ArrayList<>();

            for (Indicador indicador : indicadoresCalculados) {
                IndicadorCalculado indicadorCalculado = convertirAIndicadorCalculado(indicador, usuario);
                indicadoresParaGuardar.add(indicadorCalculado);
            }

            // 3. Guardar en lote
            List<IndicadorCalculado> indicadoresGuardados = indicadorCalculadoRepository.saveAll(indicadoresParaGuardar);
            logger.info("Se guardaron {} indicadores calculados en la colección", indicadoresGuardados.size());

            return indicadoresGuardados;

        } catch (Exception e) {
            logger.error("Error al calcular y guardar indicadores: {}", e.getMessage(), e);
            throw new RuntimeException("Error en el cálculo y guardado de indicadores: " + e.getMessage(), e);
        }
    }

    /**
     * Calcula, guarda y reemplaza los cálculos anteriores para los mismos periodos
     */
    @Transactional
    public List<IndicadorCalculado> recalcularYReemplazarIndicadores(List<String> periodos, String usuario) {
        logger.info("Iniciando recálculo y reemplazo para periodos: {}", periodos);

        try {
            // 1. Eliminar cálculos anteriores para estos periodos
            for (String periodo : periodos) {
                List<IndicadorCalculado> anteriores = indicadorCalculadoRepository.findByPeriodoActual(periodo);
                if (!anteriores.isEmpty()) {
                    indicadorCalculadoRepository.deleteAll(anteriores);
                    logger.info("Eliminados {} cálculos anteriores para periodo: {}", anteriores.size(), periodo);
                }
            }

            // 2. Calcular y guardar nuevos
            return calcularYGuardarIndicadores(periodos, usuario);

        } catch (Exception e) {
            logger.error("Error al recalcular indicadores: {}", e.getMessage(), e);
            throw new RuntimeException("Error en el recálculo de indicadores: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene los indicadores calculados guardados
     */
    public List<IndicadorCalculado> obtenerIndicadoresCalculadosGuardados() {
        return indicadorCalculadoRepository.findAll();
    }

    /**
     * Obtiene indicadores calculados por periodo
     */
    public List<IndicadorCalculado> obtenerIndicadoresCalculadosPorPeriodo(String periodo) {
        return indicadorCalculadoRepository.findByPeriodoActual(periodo);
    }

    /**
     * Obtiene indicadores calculados por negocio
     */
    public List<IndicadorCalculado> obtenerIndicadoresCalculadosPorNegocio(Integer negocio) {
        return indicadorCalculadoRepository.findLatestByNegocio(negocio);
    }

    /**
     * Elimina cálculos anteriores a una fecha
     */
    @Transactional
    public void limpiarCalculosAnteriores(LocalDateTime fechaLimite) {
        logger.info("Eliminando cálculos anteriores a: {}", fechaLimite);
        indicadorCalculadoRepository.deleteByFechaCalculoBefore(fechaLimite);
        logger.info("Limpieza de cálculos completada");
    }

    // Métodos privados auxiliares

    private IndicadorCalculado convertirAIndicadorCalculado(Indicador indicador, String usuario) {
        IndicadorCalculado calculado = new IndicadorCalculado();
        calculado.setPuesto(indicador.getPuesto());
        calculado.setSucursal(indicador.getSucursal());
        calculado.setFcDetalle5(indicador.getFcDetalle5());
        calculado.setFcDetalle6(indicador.getFcDetalle6());
        calculado.setNegocio(indicador.getNegocio());
        calculado.setConceptoDetalle(indicador.getConceptoDetalle());
        calculado.setPeriodoAnterior(indicador.getPeriodoAnterior());
        calculado.setValorAnterior(indicador.getValorAnterior());
        calculado.setPeriodoActual(indicador.getPeriodoActual());
        calculado.setValorActual(indicador.getValorActual());
        calculado.setDiferencia(indicador.getDiferencia());
        calculado.setVariacion(indicador.getVariacion());
        calculado.setUsuarioCalculo(usuario);
        calculado.setFechaCalculo(LocalDateTime.now());
        return calculado;
    }



   private List<Document> createAggregationPipeline(List<String> periodos) {
       // Ordenar períodos para consistencia
       List<String> periodosOrdenados = periodos.stream().sorted().collect(Collectors.toList());
       String periodoAnterior = periodosOrdenados.get(0);
       String periodoActual = periodosOrdenados.get(1);

        return Arrays.asList(
                new Document("$match", new Document("PkiPeriodo", new Document("$in", periodos))),

                new Document("$group", new Document("_id", new Document("puesto", "$PkiPuesto")
                        .append("sucursal", "$FcDetalle4")
                        .append("FcDetalle5", "$FcDetalle5")
                        .append("FcDetalle6", "$FcDetalle6")
                        .append("negocio", "$PkiGrupoNegocio")
                        .append("conceptoDetalle", "$PkiConceptoDetalle")
                        .append("periodo", "$PkiPeriodo"))
                        .append("valor", new Document("$sum", "$FnValor"))),

                new Document("$group", new Document("_id", new Document("puesto", "$_id.puesto")
                        .append("sucursal", "$_id.sucursal")
                        .append("FcDetalle5", "$_id.FcDetalle5")
                        .append("FcDetalle6", "$_id.FcDetalle6")
                        .append("negocio", "$_id.negocio")
                        .append("conceptoDetalle", "$_id.conceptoDetalle"))
                        .append("valores", new Document("$push", new Document("periodo", "$_id.periodo")
                                .append("valor", "$valor")))),

                new Document("$unwind", "$valores"),
                new Document("$sort", new Document("valores.periodo", 1)),

                new Document("$group", new Document("_id", new Document("puesto", "$_id.puesto")
                        .append("sucursal", "$_id.sucursal")
                        .append("FcDetalle5", "$_id.FcDetalle5")
                        .append("FcDetalle6", "$_id.FcDetalle6")
                        .append("negocio", "$_id.negocio")
                        .append("conceptoDetalle", "$_id.conceptoDetalle"))
                        .append("valores", new Document("$push", "$valores"))),

                new Document("$project", new Document("puesto", "$_id.puesto")
                        .append("sucursal", "$_id.sucursal")
                        .append("FcDetalle5", "$_id.FcDetalle5")
                        .append("FcDetalle6", "$_id.FcDetalle6")
                        .append("negocio", "$_id.negocio")
                        .append("conceptoDetalle", "$_id.conceptoDetalle")
                        /*.append("PeriodoAnterior", new Document("$arrayElemAt", Arrays.asList("$valores.periodo", 0)))
                        .append("valorAnterior", new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0)))
                        .append("PeriodoActual", new Document("$arrayElemAt", Arrays.asList("$valores.periodo", 1)))
                        .append("valorActual", new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)))
                        */

                        // CAMBIO CLAVE: forzar períodos correctos
                        .append("PeriodoAnterior", periodoAnterior)
                        .append("valorAnterior", new Document("$ifNull", Arrays.asList(
                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0)), 0)))

                        .append("PeriodoActual", periodoActual)  // <- ESTE ES EL CAMBIO PRINCIPAL
                        .append("valorActual", new Document("$ifNull", Arrays.asList(
                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)), 0)))



                        .append("diferencia", new Document("$subtract", Arrays.asList(
                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)),
                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0))
                        )))
                        .append("variacion", new Document("$cond", new Document("if",
                                new Document("$eq", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0)), 0)))
                                .append("then", 0)
                                .append("else", new Document("$multiply", Arrays.asList(
                                        new Document("$divide", Arrays.asList(
                                                new Document("$subtract", Arrays.asList(
                                                        new Document("$arrayElemAt", Arrays.asList("$valores.valor", 1)),
                                                        new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0))
                                                )),
                                                new Document("$arrayElemAt", Arrays.asList("$valores.valor", 0))
                                        )),
                        100
                )))))));
    }



    private Double getDoubleValue(Document document, String field) {
        Object value = document.get(field);
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else {
            return 0.0;
        }
    }
}