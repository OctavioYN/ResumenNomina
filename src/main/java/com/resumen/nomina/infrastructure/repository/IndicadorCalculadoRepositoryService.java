package com.resumen.nomina.infrastructure.repository;


import com.resumen.nomina.application.util.DocumentHelper;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class IndicadorCalculadoRepositoryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Obtiene indicadores completos para un período específico
     * CAMBIO PRINCIPAL: Ahora solo requiere periodoActual como parámetro
     */
    public List<Document> obtenerIndicadoresCompletos(String periodoActual) {
        log.info("=== INICIANDO PIPELINE PARA PERÍODO {} ===", periodoActual);

        // Pipeline optimizado - SOLO requiere periodoActual
        List<Document> pipeline = Arrays.asList(
                // 1. Match - CAMBIO: Solo filtrar por período actual
                new Document("$match", new Document()
                        .append("periodoActual", periodoActual)  // <- ÚNICO PARÁMETRO
                        .append("fcDetalle6", new Document("$regex", "^(Compensación|Empleado)")
                                .append("$options", "i"))),

                // 2. Group - CAMBIO: periodoAnterior viene del documento
                new Document("$group", new Document("_id", new Document()
                        .append("puesto", "$puesto")
                        .append("sucursal", "$sucursal")
                        .append("negocio", "$negocio")
                        .append("fcDetalle5", "$fcDetalle5")
                        .append("periodoActual", "$periodoActual")
                        .append("periodoAnterior", "$periodoAnterior"))  // <- Ya viene en el documento

                        // Totales de compensación
                        .append("compensacionActual", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$regexMatch", new Document("input", "$fcDetalle6")
                                        .append("regex", "Compensación").append("options", "i")),
                                "$valorActual", 0))))
                        .append("compensacionAnterior", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$regexMatch", new Document("input", "$fcDetalle6")
                                        .append("regex", "Compensación").append("options", "i")),
                                "$valorAnterior", 0))))

                        // Totales de empleados
                        .append("empleadosActual", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$regexMatch", new Document("input", "$fcDetalle6")
                                        .append("regex", "Empleado").append("options", "i")),
                                "$valorActual", 0))))
                        .append("empleadosAnterior", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$regexMatch", new Document("input", "$fcDetalle6")
                                        .append("regex", "Empleado").append("options", "i")),
                                "$valorAnterior", 0))))),

                // 3. Project - Calcular métricas finales
                new Document("$project", new Document("_id", 0)
                        // Información básica
                        .append("puesto", "$_id.puesto")
                        .append("sucursal", new Document("$trim", new Document("input", "$_id.sucursal")))
                        .append("fcDetalle5", new Document("$trim", new Document("input", "$_id.fcDetalle5")))
                        .append("negocio", "$_id.negocio")
                        .append("periodoAnterior", "$_id.periodoAnterior")
                        .append("periodoActual", "$_id.periodoActual")

                        // === DATOS PARA VISTA TOTAL ===
                        .append("valorAnterior", new Document("$round", Arrays.asList("$compensacionAnterior", 2)))
                        .append("valorActual", new Document("$round", Arrays.asList("$compensacionActual", 2)))
                        .append("diferencia", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList("$compensacionActual", "$compensacionAnterior")), 2)))
                        .append("variacion", new Document("$round", Arrays.asList(
                                new Document("$cond", new Document()
                                        .append("if", new Document("$gt", Arrays.asList("$compensacionAnterior", 0)))
                                        .append("then", new Document("$multiply", Arrays.asList(
                                                new Document("$divide", Arrays.asList(
                                                        new Document("$subtract", Arrays.asList("$compensacionActual", "$compensacionAnterior")),
                                                        "$compensacionAnterior")),
                                                100)))
                                        .append("else", 0)), 2)))

                        // === DATOS PARA VISTA PROMEDIO ===
                        .append("promedioAnterior", new Document("$round", Arrays.asList(
                                new Document("$cond", new Document()
                                        .append("if", new Document("$gt", Arrays.asList("$empleadosAnterior", 0)))
                                        .append("then", new Document("$divide", Arrays.asList("$compensacionAnterior", "$empleadosAnterior")))
                                        .append("else", 0)), 2)))
                        .append("promedioActual", new Document("$round", Arrays.asList(
                                new Document("$cond", new Document()
                                        .append("if", new Document("$gt", Arrays.asList("$empleadosActual", 0)))
                                        .append("then", new Document("$divide", Arrays.asList("$compensacionActual", "$empleadosActual")))
                                        .append("else", 0)), 2)))
                        .append("diferenciaPromedio", new Document("$round", Arrays.asList(
                                new Document("$subtract", Arrays.asList(
                                        new Document("$cond", new Document()
                                                .append("if", new Document("$gt", Arrays.asList("$empleadosActual", 0)))
                                                .append("then", new Document("$divide", Arrays.asList("$compensacionActual", "$empleadosActual")))
                                                .append("else", 0)),
                                        new Document("$cond", new Document()
                                                .append("if", new Document("$gt", Arrays.asList("$empleadosAnterior", 0)))
                                                .append("then", new Document("$divide", Arrays.asList("$compensacionAnterior", "$empleadosAnterior")))
                                                .append("else", 0)))), 2)))
                        .append("variacionPromedio", new Document("$round", Arrays.asList(
                                new Document("$cond", new Document()
                                        .append("if", new Document("$and", Arrays.asList(
                                                new Document("$gt", Arrays.asList("$empleadosAnterior", 0)),
                                                new Document("$gt", Arrays.asList("$compensacionAnterior", 0)))))
                                        .append("then", new Document("$multiply", Arrays.asList(
                                                new Document("$divide", Arrays.asList(
                                                        new Document("$subtract", Arrays.asList(
                                                                new Document("$divide", Arrays.asList("$compensacionActual", "$empleadosActual")),
                                                                new Document("$divide", Arrays.asList("$compensacionAnterior", "$empleadosAnterior")))),
                                                        new Document("$divide", Arrays.asList("$compensacionAnterior", "$empleadosAnterior")))),
                                                100)))
                                        .append("else", 0)), 2)))

                        // Información adicional
                        .append("empleadosActual", "$empleadosActual")
                        .append("empleadosAnterior", "$empleadosAnterior")),

                // 4. Sort - Ordenar resultados
                new Document("$sort", new Document()
                        .append("puesto", 1)
                        .append("sucursal", 1))
        );

        log.info("Ejecutando pipeline con {} stages para período {}", pipeline.size(), periodoActual);

        List<Document> resultados = ejecutarPipeline(pipeline);
        log.info("Resultados obtenidos: {}", resultados.size());

        if (!resultados.isEmpty()) {
            log.info("Primer resultado: {}", resultados.get(0).toJson());
        }

        return resultados;
    }

    /**
     * NUEVO MÉTODO: Verifica los datos disponibles para un período
     */
    public Map<String, Object> verificarDatos(String periodoActual) {
        log.info("=== VERIFICANDO DATOS PARA PERÍODO {} ===", periodoActual);

        Map<String, Object> verificacion = new HashMap<>();

        try {
            // Contar total de documentos
            long total = mongoTemplate.getCollection("IndicadoresCalculados").countDocuments();
            verificacion.put("totalDocumentos", total);

            // Buscar por período actual
            Document queryPeriodo = new Document("periodoActual", periodoActual);
            long conPeriodo = mongoTemplate.getCollection("IndicadoresCalculados").countDocuments(queryPeriodo);
            verificacion.put("documentosConPeriodo", conPeriodo);

            // Buscar compensación
            Document queryComp = new Document()
                    .append("periodoActual", periodoActual)
                    .append("fcDetalle6", new Document("$regex", "Compensación").append("$options", "i"));
            long conComp = mongoTemplate.getCollection("IndicadoresCalculados").countDocuments(queryComp);
            verificacion.put("documentosCompensacion", conComp);

            // Buscar empleados
            Document queryEmp = new Document()
                    .append("periodoActual", periodoActual)
                    .append("fcDetalle6", new Document("$regex", "Empleado").append("$options", "i"));
            long conEmp = mongoTemplate.getCollection("IndicadoresCalculados").countDocuments(queryEmp);
            verificacion.put("documentosEmpleados", conEmp);

            // Obtener ejemplos
            List<Document> ejemplos = mongoTemplate.execute("IndicadoresCalculados", collection -> {
                return collection.find(queryPeriodo).limit(3).into(new ArrayList<>());
            });

            List<Map<String, Object>> ejemplosSimplificados = new ArrayList<>();
            for (int i = 0; i < ejemplos.size(); i++) {
                Document doc = ejemplos.get(i);
                Map<String, Object> ejemplo = new HashMap<>();
                ejemplo.put("puesto", doc.get("puesto"));
                ejemplo.put("fcDetalle6", doc.getString("fcDetalle6"));
                ejemplo.put("valorActual", doc.get("valorActual"));
                ejemplo.put("valorAnterior", doc.get("valorAnterior"));
                ejemplo.put("periodoAnterior", doc.getString("periodoAnterior"));
                ejemplosSimplificados.add(ejemplo);
            }
            verificacion.put("ejemplos", ejemplosSimplificados);

            log.info("Verificación completada: Total={}, ConPeriodo={}, Compensación={}, Empleados={}",
                    total, conPeriodo, conComp, conEmp);

        } catch (Exception e) {
            log.error("Error durante la verificación de datos: {}", e.getMessage(), e);
            verificacion.put("error", "Error durante la verificación: " + e.getMessage());
        }

        return verificacion;
    }

    /**
     * Ejecuta el pipeline de agregación en la colección
     */
    private List<Document> ejecutarPipeline(List<Document> pipeline) {
        return mongoTemplate.execute("IndicadoresCalculados", collection -> {
            log.debug("Ejecutando en colección: {}", collection.getNamespace());
            return collection.aggregate(pipeline).into(new ArrayList<>());
        });
    }




    //Agrega nuevo para graficas por promedio

    /**
     * Calcula promedio histórico por negocio, puesto e indicador (conceptoDetalle)
     * Similar al método existente pero enfocado en indicadores específicos
     */
    public List<Document> obtenerPromediosIndicadorHistorico(Integer negocio, Integer puesto,
                                                             Integer conceptoDetalle, Integer anioDesde) {
        log.info("=== CALCULANDO PROMEDIOS HISTÓRICOS: Negocio={}, Puesto={}, Indicador={}, Desde={} ===",
                negocio, puesto, conceptoDetalle, anioDesde);

        String regexAnio = "^" + anioDesde + "|^" + (anioDesde + 1) + "|^" + (anioDesde + 2);

        

        List<Document> pipeline = Arrays.asList(
                // 1. Filtrar por negocio, puesto y año
                new Document("$match", new Document()
                        .append("negocio", negocio)
                        .append("puesto", puesto)
                        .append("periodoActual", new Document("$regex", regexAnio))
                        .append("conceptoDetalle", new Document("$in", Arrays.asList(1011, conceptoDetalle)))),

                // 2. Agregar campo semana
                new Document("$addFields", new Document()
                        .append("semana", new Document("$concat", Arrays.asList(
                                new Document("$substr", Arrays.asList("$periodoActual", 0, 4)),
                                "-",
                                new Document("$substr", Arrays.asList("$periodoActual", 4, 2)))))
                        .append("anio", new Document("$toInt",
                                new Document("$substr", Arrays.asList("$periodoActual", 0, 4))))
                        .append("numeroSemana", new Document("$toInt",
                                new Document("$substr", Arrays.asList("$periodoActual", 4, 2))))),

                // 3. Agrupar por semana y conceptoDetalle
                new Document("$group", new Document("_id", new Document()
                        .append("semana", "$semana")
                        .append("anio", "$anio")
                        .append("numeroSemana", "$numeroSemana")
                        .append("conceptoDetalle", "$conceptoDetalle"))
                        .append("totalValor", new Document("$sum", "$valorActual"))
                        .append("totalRegistros", new Document("$sum", 1))),

                // 4. Reagrupar por semana para tener indicador y empleados juntos
                new Document("$group", new Document("_id", new Document()
                        .append("semana", "$_id.semana")
                        .append("anio", "$_id.anio")
                        .append("numeroSemana", "$_id.numeroSemana"))
                        .append("indicadores", new Document("$push", new Document()
                                .append("conceptoDetalle", "$_id.conceptoDetalle")
                                .append("totalValor", "$totalValor")
                                .append("totalRegistros", "$totalRegistros")))),

                // 5. Calcular promedio (indicador / empleados)
                new Document("$addFields", new Document()
                        .append("empleados", new Document("$let", new Document()
                                .append("vars", new Document("emp", new Document("$filter", new Document()
                                        .append("input", "$indicadores")
                                        .append("cond", new Document("$eq", Arrays.asList("$$this.conceptoDetalle", 1011))))))
                                .append("in", new Document("$arrayElemAt", Arrays.asList("$$emp.totalValor", 0)))))
                        .append("indicadorEspecifico", new Document("$let", new Document()
                                .append("vars", new Document("ind", new Document("$filter", new Document()
                                        .append("input", "$indicadores")
                                        .append("cond", new Document("$eq", Arrays.asList("$$this.conceptoDetalle", conceptoDetalle))))))
                                .append("in", new Document("$arrayElemAt", Arrays.asList("$$ind.totalValor", 0)))))),

                // 6. Proyectar resultado final
                new Document("$project", new Document("_id", 0)
                        .append("semana", "$_id.semana")
                        .append("anio", "$_id.anio")
                        .append("numeroSemana", "$_id.numeroSemana")
                        .append("totalIndicador", "$indicadorEspecifico")
                        .append("totalEmpleados", "$empleados")
                        .append("promedio", new Document("$cond", new Document()
                                .append("if", new Document("$gt", Arrays.asList("$empleados", 0)))
                                .append("then", new Document("$round", Arrays.asList(
                                        new Document("$divide", Arrays.asList("$indicadorEspecifico", "$empleados")), 2)))
                                .append("else", 0)))),

                // 7. Ordenar cronológicamente
                new Document("$sort", new Document("anio", 1).append("numeroSemana", 1))
        );

        return ejecutarPipeline(pipeline);
    }

    /**
     * Calcula estadísticas históricas para un indicador específico
     */
    public Document calcularEstadisticasIndicador(Integer negocio, Integer puesto,
                                                  Integer conceptoDetalle, Integer anioDesde) {
        log.info("=== CALCULANDO ESTADÍSTICAS: Negocio={}, Puesto={}, Indicador={} ===",
                negocio, puesto, conceptoDetalle);

        List<Document> datos = obtenerPromediosIndicadorHistorico(negocio, puesto, conceptoDetalle, anioDesde);

        if (datos.isEmpty()) {
            return null;
        }

        // Extraer solo los promedios para cálculos estadísticos
        List<Double> promedios = new ArrayList<>();
        for (Document doc : datos) {
            //Double promedio = doc.getDouble("promedio");
            Double promedio = DocumentHelper.getDoubleValue(doc, "promedio"); // ✅ Maneja Integer y Double
            if (promedio != null && promedio > 0) {
                promedios.add(promedio);
            }
        }

        if (promedios.isEmpty()) {
            return null;
        }

        // Calcular media
        double media = promedios.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calcular desviación estándar
        double varianza = promedios.stream()
                .mapToDouble(p -> Math.pow(p - media, 2))
                .average()
                .orElse(0.0);
        double desviacionEstandar = Math.sqrt(varianza);

        // Construir documento de estadísticas
        Document estadisticas = new Document()
                .append("media", Math.round(media * 100.0) / 100.0)
                .append("desviacionEstandar", Math.round(desviacionEstandar * 100.0) / 100.0)
                .append("totalSemanas", promedios.size())
                .append("superior1DS", Math.round((media + desviacionEstandar) * 100.0) / 100.0)
                .append("inferior1DS", Math.round((media - desviacionEstandar) * 100.0) / 100.0)
                .append("superior15DS", Math.round((media + (desviacionEstandar * 1.5)) * 100.0) / 100.0)
                .append("inferior15DS", Math.round((media - (desviacionEstandar * 1.5)) * 100.0) / 100.0);

        log.info("Estadísticas calculadas: Media={}, DS={}, Semanas={}",
                estadisticas.getDouble("media"),
                estadisticas.getDouble("desviacionEstandar"),
                estadisticas.getInteger("totalSemanas"));

        return estadisticas;
    }

    /**
     * Lista las combinaciones disponibles de negocio-puesto-indicador
     */
    public List<Document> listarCombinacionesDisponibles() {
        log.info("=== LISTANDO COMBINACIONES DISPONIBLES ===");

        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("conceptoDetalle", new Document("$ne", 1011))),
                new Document("$group", new Document("_id", new Document()
                        .append("negocio", "$negocio")
                        .append("puesto", "$puesto")
                        .append("conceptoDetalle", "$conceptoDetalle"))
                        .append("totalRegistros", new Document("$sum", 1))),
                new Document("$project", new Document("_id", 0)
                        .append("negocio", "$_id.negocio")
                        .append("puesto", "$_id.puesto")
                        .append("conceptoDetalle", "$_id.conceptoDetalle")
                        .append("totalRegistros", 1)),
                new Document("$sort", new Document("negocio", 1)
                        .append("puesto", 1)
                        .append("conceptoDetalle", 1)),
                new Document("$limit", 100)
        );

        return ejecutarPipeline(pipeline);
    }


}