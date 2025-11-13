package com.resumen.nomina.admin.service;

import com.resumen.nomina.admin.dto.CatalogoRow;
import com.resumen.nomina.admin.dto.IndicadorRow;
import com.resumen.nomina.admin.dto.PuestoRow;
import com.resumen.nomina.admin.dto.ResultadoCarga;
import com.resumen.nomina.domain.model.IndicadorMenu;
import com.resumen.nomina.domain.model.Menu;
import com.resumen.nomina.domain.model.Puesto;
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
import java.util.stream.Collectors; /**

 /**
 * 📚 SERVICIO PARA ADMINISTRAR CATÁLOGO TEXTOS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCatalogoService {

    private final MongoTemplate mongoTemplate;
    private final ExcelProcessorService excelProcessor;

    /**
     * Carga masiva de CatalogoTextos desde Excel
     */
    @Transactional
    public ResultadoCarga cargarCatalogo(MultipartFile file, String usuario, boolean reemplazar) {
        log.info("📚 Iniciando carga de CatalogoTextos - Reemplazar: {} - Usuario: {}",
                reemplazar, usuario);

        long inicio = System.currentTimeMillis();
        ResultadoCarga resultado = ResultadoCarga.builder()
                .fechaCarga(LocalDateTime.now())
                .usuarioCarga(usuario)
                .archivoOriginal(file.getOriginalFilename())
                .errores(new ArrayList<>())
                .build();

        try {
            // 1. Procesar Excel
            List<CatalogoRow> catalogos = excelProcessor.procesarExcelCatalogo(file);
            resultado.setTotalRegistros(catalogos.size());

            if (catalogos.isEmpty()) {
                resultado.setSuccess(false);
                resultado.setMensaje("No se encontraron registros válidos");
                return resultado;
            }

            // 2. Si reemplazar = true, eliminar existentes
            if (reemplazar) {
                log.warn("⚠️ ELIMINANDO catálogos existentes...");
                mongoTemplate.dropCollection("catalogoTextos");
            }

            // 3. Convertir y guardar
            int exitosos = 0;
            int errores = 0;

            for (CatalogoRow catalogo : catalogos) {
                try {
                    Menu menu = convertirAMenu(catalogo);

                    if (reemplazar) {
                        // Insertar directamente
                        mongoTemplate.save(menu, "catalogoTextos");
                    } else {
                        // Verificar si existe y actualizar/insertar
                        Document filtro = new Document("id", catalogo.getId());
                        long existe = mongoTemplate.getCollection("catalogoTextos")
                                .countDocuments(filtro);

                        if (existe > 0) {
                            mongoTemplate.getCollection("catalogoTextos")
                                    .replaceOne(filtro, convertirMenuADocument(menu));
                        } else {
                            mongoTemplate.save(menu, "catalogoTextos");
                        }
                    }

                    exitosos++;
                } catch (Exception e) {
                    errores++;
                    resultado.getErrores().add("Error con catálogo ID " +
                            catalogo.getId() + ": " + e.getMessage());
                }
            }

            resultado.setRegistrosExitosos(exitosos);
            resultado.setRegistrosConError(errores);
            resultado.setSuccess(errores == 0);
            resultado.setMensaje(String.format("Carga completada: %d exitosos, %d errores",
                    exitosos, errores));

            long fin = System.currentTimeMillis();
            resultado.setTiempoProcesamientoMs(fin - inicio);

        } catch (Exception e) {
            log.error("❌ Error cargando catálogo: {}", e.getMessage(), e);
            resultado.setSuccess(false);
            resultado.setMensaje("Error: " + e.getMessage());
        }

        return resultado;
    }

    private Menu convertirAMenu(CatalogoRow catalogo) {
        Menu menu = new Menu();
        menu.setId(catalogo.getId());
        menu.setNegocio(catalogo.getNegocio());

        List<Puesto> puestos = catalogo.getPuestos().stream()
                .map(this::convertirAPuesto)
                .collect(Collectors.toList());

        menu.setPuestos(puestos);
        return menu;
    }

    private Puesto convertirAPuesto(PuestoRow puestoRow) {
        Puesto puesto = new Puesto();
        puesto.setIdPuesto(puestoRow.getIdPuesto());
        puesto.setIdFuncion(puestoRow.getIdFuncion());
        puesto.setPuesto(puestoRow.getPuesto());

        List<IndicadorMenu> indicadores = puestoRow.getIndicadores().stream()
                .map(this::convertirAIndicador)
                .collect(Collectors.toList());

        puesto.setIndicadores(indicadores);
        return puesto;
    }

    private IndicadorMenu convertirAIndicador(IndicadorRow indicadorRow) {
        return new IndicadorMenu(
                indicadorRow.getIdIndicador(),
                indicadorRow.getIndicador()
        );
    }

    private Document convertirMenuADocument(Menu menu) {
        // Implementar conversión manual si es necesario
        // O usar mongoTemplate.getConverter().convertToMongoType(menu)
        return (Document) mongoTemplate.getConverter().convertToMongoType(menu);
    }
}