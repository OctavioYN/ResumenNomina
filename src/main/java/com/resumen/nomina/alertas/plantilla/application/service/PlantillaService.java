package com.resumen.nomina.alertas.plantilla.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaService {

    private final MongoTemplate mongoTemplate;

    public List<Map<String, Object>> obtenerDatosActivos(String periodoActual) {
        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document()
                        .append("conceptoDetalle", 1011)
                        .append("periodoActual", periodoActual)
                ),
                new Document("$project", new Document()
                        .append("puesto", "$fcDetalle5")
                        .append("valorActual", "$valorActual")
                        .append("periodoAnterior", "$periodoAnterior")
                        .append("valorAnterior", "$valorAnterior")
                        .append("diferencia", new Document("$subtract",
                                Arrays.asList("$valorActual", "$valorAnterior")))
                ),
                new Document("$sort", new Document("puesto", 1))
        );

        try {
            List<Document> results = mongoTemplate.getDb()
                    .getCollection("IndicadoresCalculados")
                    .aggregate(pipeline)
                    .into(new ArrayList<>());

            List<Map<String, Object>> respuesta = new ArrayList<>();
            for (Document doc : results) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("Puesto", doc.getString("puesto"));
                item.put("Activos - Post Cálculos", doc.getDouble("valorActual"));
                item.put("Activos - Weekly", doc.getDouble("valorAnterior"));
                item.put("Diferencia", doc.getDouble("diferencia"));
                respuesta.add(item);
            }

            log.info("✅ Datos de activos obtenidos: {} registros", respuesta.size());
            return respuesta;

        } catch (Exception e) {
            log.error("❌ Error en consulta MongoDB: {}", e.getMessage());
            throw new RuntimeException("Error obteniendo datos de activos: " + e.getMessage());
        }
    }
}