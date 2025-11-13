package com.resumen.nomina.admin.service;

import com.resumen.nomina.admin.dto.*;
import com.resumen.nomina.domain.model.Menu;
import com.resumen.nomina.domain.model.Puesto;
import com.resumen.nomina.domain.model.IndicadorMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🗄️ SERVICIO PARA ADMINISTRAR DATOS INTELIGENCIA
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDatosInteligenciaService {

    private final MongoTemplate mongoTemplate;
    private final ExcelProcessorService excelProcessor;

    /**
     * Carga masiva de DatosInteligencia desde Excel
     * OPCIÓN 1: Reemplazar todos los datos
     */
    @Transactional
    public ResultadoCarga cargarDatosReemplazar(MultipartFile file, String usuario) {
        log.info("🔄 Iniciando carga COMPLETA de DatosInteligencia por usuario: {}", usuario);

        long inicio = System.currentTimeMillis();
        ResultadoCarga resultado = ResultadoCarga.builder()
                .fechaCarga(LocalDateTime.now())
                .usuarioCarga(usuario)
                .archivoOriginal(file.getOriginalFilename())
                .errores(new ArrayList<>())
                .build();

        try {
            // 1. Procesar Excel
            List<DatosInteligenciaRow> datos = excelProcessor.procesarExcelDatosInteligencia(file);
            resultado.setTotalRegistros(datos.size());

            if (datos.isEmpty()) {
                resultado.setSuccess(false);
                resultado.setMensaje("No se encontraron registros válidos en el archivo");
                return resultado;
            }

            // 2. ELIMINAR todos los datos existentes
            log.warn("⚠️ ELIMINANDO todos los registros existentes de DatosInteligencia...");
            long eliminados = mongoTemplate.getCollection("DatosInteligencia").countDocuments();
            mongoTemplate.dropCollection("DatosInteligencia");
            log.info("🗑️ Eliminados {} registros", eliminados);

            // 3. Convertir a Documents e insertar
            List<Document> documents = datos.stream()
                    .map(this::convertirADocument)
                    .collect(Collectors.toList());

            mongoTemplate.getCollection("DatosInteligencia").insertMany(documents);

            resultado.setRegistrosExitosos(documents.size());
            resultado.setSuccess(true);
            resultado.setMensaje("Carga completa exitosa: " + documents.size() + " registros insertados");

            long fin = System.currentTimeMillis();
            resultado.setTiempoProcesamientoMs(fin - inicio);

            log.info("✅ Carga completada en {} ms", resultado.getTiempoProcesamientoMs());

        } catch (Exception e) {
            log.error("❌ Error durante la carga: {}", e.getMessage(), e);
            resultado.setSuccess(false);
            resultado.setMensaje("Error durante la carga: " + e.getMessage());
            resultado.getErrores().add(e.getMessage());
        }

        return resultado;
    }

    /**
     * OPCIÓN 2: Carga incremental (solo agregar/actualizar)
     */
    @Transactional
    public ResultadoCarga cargarDatosIncremental(MultipartFile file, String usuario) {
        log.info("➕ Iniciando carga INCREMENTAL de DatosInteligencia por usuario: {}", usuario);

        long inicio = System.currentTimeMillis();
        ResultadoCarga resultado = ResultadoCarga.builder()
                .fechaCarga(LocalDateTime.now())
                .usuarioCarga(usuario)
                .archivoOriginal(file.getOriginalFilename())
                .errores(new ArrayList<>())
                .build();

        try {
            List<DatosInteligenciaRow> datos = excelProcessor.procesarExcelDatosInteligencia(file);
            resultado.setTotalRegistros(datos.size());

            if (datos.isEmpty()) {
                resultado.setSuccess(false);
                resultado.setMensaje("No se encontraron registros válidos");
                return resultado;
            }

            // Insertar o actualizar cada registro
            int exitosos = 0;
            int errores = 0;

            for (DatosInteligenciaRow dato : datos) {
                try {
                    Document doc = convertirADocument(dato);

                    // Crear filtro único por: periodo + puesto + concepto + negocio
                    // (los campos que identifican un registro único)
                    Document filtro = new Document()
                            .append("PkiPeriodo", dato.getPkiPeriodo())
                            .append("PkiPuesto", dato.getPkiPuesto())
                            .append("PkiGrupoNegocio", dato.getPkiGrupoNegocio())
                            .append("PkiConceptoDetalle", dato.getPkiConceptoDetalle())
                            .append("PkiSucursal", dato.getPkiSucursal())
                            .append("PkiEmpleado", dato.getPkiEmpleado());

                    // Upsert (actualizar si existe, insertar si no)
                    mongoTemplate.getCollection("DatosInteligencia")
                            .replaceOne(filtro, doc,
                                    new com.mongodb.client.model.ReplaceOptions().upsert(true));

                    exitosos++;
                } catch (Exception e) {
                    errores++;
                    resultado.getErrores().add("Error insertando registro: " + e.getMessage());
                }
            }

            resultado.setRegistrosExitosos(exitosos);
            resultado.setRegistrosConError(errores);
            resultado.setSuccess(errores == 0);
            resultado.setMensaje(String.format("Carga incremental: %d exitosos, %d errores",
                    exitosos, errores));

            long fin = System.currentTimeMillis();
            resultado.setTiempoProcesamientoMs(fin - inicio);

        } catch (Exception e) {
            log.error("❌ Error durante la carga incremental: {}", e.getMessage(), e);
            resultado.setSuccess(false);
            resultado.setMensaje("Error: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Eliminar datos por período
     */
    @Transactional
    public ResultadoCarga eliminarPorPeriodo(String periodo, String usuario) {
        log.info("🗑️ Eliminando datos del período: {} por usuario: {}", periodo, usuario);

        ResultadoCarga resultado = ResultadoCarga.builder()
                .fechaCarga(LocalDateTime.now())
                .usuarioCarga(usuario)
                .build();

        try {
            Document filtro = new Document("PkiPeriodo", periodo);
            long eliminados = mongoTemplate.getCollection("DatosInteligencia")
                    .deleteMany(filtro)
                    .getDeletedCount();

            resultado.setSuccess(true);
            resultado.setTotalRegistros((int) eliminados);
            resultado.setMensaje("Eliminados " + eliminados + " registros del período " + periodo);

        } catch (Exception e) {
            resultado.setSuccess(false);
            resultado.setMensaje("Error: " + e.getMessage());
        }

        return resultado;
    }

    private Document convertirADocument(DatosInteligenciaRow dato) {
        return new Document()
                // Campos PKI
                .append("PkiPuesto", dato.getPkiPuesto())
                .append("PkiSucursal", dato.getPkiSucursal())
                .append("PkiEmpleado", dato.getPkiEmpleado())
                .append("PkiCDGenerico", dato.getPkiCDGenerico())
                .append("PkiPais", dato.getPkiPais())
                .append("PkiPeriodo", dato.getPkiPeriodo())
                .append("PkiGrupoNegocio", dato.getPkiGrupoNegocio())
                .append("PkiCanal", dato.getPkiCanal())
                .append("PkiConceptoDetalle", dato.getPkiConceptoDetalle())
                // Campos FN
                .append("FnValor", dato.getFnValor())
                .append("FnDetalle1", dato.getFnDetalle1())
                .append("FnDetalle2", dato.getFnDetalle2())
                // Campo PK
                .append("PkcDetalle3", dato.getPkcDetalle3())
                // Campos FC (con espacios en blanco al final para mantener formato original)
                .append("FcDetalle4", String.format("%-50s", dato.getFcDetalle4()))
                .append("FcDetalle5", String.format("%-50s", dato.getFcDetalle5()))
                .append("FcDetalle6", String.format("%-50s", dato.getFcDetalle6()))
                .append("FnDetalle7", dato.getFnDetalle7());
    }
}
