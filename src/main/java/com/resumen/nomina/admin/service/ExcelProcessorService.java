package com.resumen.nomina.admin.service;

import com.resumen.nomina.admin.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 📊 SERVICIO PARA PROCESAR ARCHIVOS EXCEL
 * Maneja la lectura y validación de archivos Excel para carga masiva
 */
@Slf4j
@Service
public class ExcelProcessorService {

    /**
     * Procesa archivo Excel de DatosInteligencia
     *
     * Formato esperado del Excel (17 columnas):
     * | PkiPuesto | PkiSucursal | PkiEmpleado | PkiCDGenerico | PkiPais | PkiPeriodo |
     * | PkiGrupoNegocio | PkiCanal | PkiConceptoDetalle | FnValor | FnDetalle1 | FnDetalle2 |
     * | PkcDetalle3 | FcDetalle4 | FcDetalle5 | FcDetalle6 | FnDetalle7 |
     */
    public List<DatosInteligenciaRow> procesarExcelDatosInteligencia(MultipartFile file) throws IOException {
        log.info("📂 Procesando Excel de DatosInteligencia: {}", file.getOriginalFilename());

        List<DatosInteligenciaRow> datos = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Validar que tenga al menos 2 filas (header + 1 dato)
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("El archivo debe contener al menos un registro de datos");
            }

            // Leer desde la fila 1 (0 es el header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    // Leer las 17 columnas en el orden correcto
                    DatosInteligenciaRow dato = DatosInteligenciaRow.builder()
                            .pkiPuesto(getCellValueAsInteger(row.getCell(0)))           // A: PkiPuesto
                            .pkiSucursal(getCellValueAsInteger(row.getCell(1)))         // B: PkiSucursal
                            .pkiEmpleado(getCellValueAsInteger(row.getCell(2)))         // C: PkiEmpleado
                            .pkiCDGenerico(getCellValueAsInteger(row.getCell(3)))       // D: PkiCDGenerico
                            .pkiPais(getCellValueAsInteger(row.getCell(4)))             // E: PkiPais
                            .pkiPeriodo(getCellValueAsString(row.getCell(5)))           // F: PkiPeriodo
                            .pkiGrupoNegocio(getCellValueAsInteger(row.getCell(6)))     // G: PkiGrupoNegocio
                            .pkiCanal(getCellValueAsInteger(row.getCell(7)))            // H: PkiCanal
                            .pkiConceptoDetalle(getCellValueAsInteger(row.getCell(8)))  // I: PkiConceptoDetalle
                            .fnValor(getCellValueAsDouble(row.getCell(9)))              // J: FnValor
                            .fnDetalle1(getCellValueAsDouble(row.getCell(10)))          // K: FnDetalle1
                            .fnDetalle2(getCellValueAsDouble(row.getCell(11)))          // L: FnDetalle2
                            .pkcDetalle3(getCellValueAsInteger(row.getCell(12)))        // M: PkcDetalle3
                            .fcDetalle4(getCellValueAsString(row.getCell(13)))          // N: FcDetalle4
                            .fcDetalle5(getCellValueAsString(row.getCell(14)))          // O: FcDetalle5
                            .fcDetalle6(getCellValueAsString(row.getCell(15)))          // P: FcDetalle6
                            .fnDetalle7(getCellValueAsDouble(row.getCell(16)))          // Q: FnDetalle7
                            .build();

                    // Validación básica
                    if (validarDatosInteligencia(dato, i)) {
                        datos.add(dato);
                    } else {
                        errores.add("Fila " + (i + 1) + ": Datos inválidos o incompletos");
                    }

                } catch (Exception e) {
                    String error = "Fila " + (i + 1) + ": " + e.getMessage();
                    log.error(error);
                    errores.add(error);
                }
            }

            log.info("✅ Procesados {} registros exitosamente", datos.size());
            if (!errores.isEmpty()) {
                log.warn("⚠️ Se encontraron {} errores durante el procesamiento", errores.size());
                errores.forEach(log::warn);
            }

        } catch (IOException e) {
            log.error("❌ Error leyendo archivo Excel: {}", e.getMessage());
            throw new IOException("Error procesando archivo Excel: " + e.getMessage(), e);
        }

        return datos;
    }

    /**
     * Procesa archivo Excel de CatalogoTextos (Menús)
     *
     * Formato esperado:
     * | ID | Negocio | IdPuesto | IdFuncion | Puesto | IdIndicador | Indicador |
     *
     * Los registros con mismo ID y Negocio se agrupan automáticamente
     */
    public List<CatalogoRow> procesarExcelCatalogo(MultipartFile file) throws IOException {
        log.info("📂 Procesando Excel de CatalogoTextos: {}", file.getOriginalFilename());

        List<CatalogoRow> catalogos = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("El archivo debe contener al menos un registro");
            }

            // Estructura temporal para agrupar
            java.util.Map<String, CatalogoRow> catalogoMap = new java.util.LinkedHashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    Integer id = getCellValueAsInteger(row.getCell(0));
                    String negocio = getCellValueAsString(row.getCell(1));
                    Integer idPuesto = getCellValueAsInteger(row.getCell(2));
                    Integer idFuncion = getCellValueAsInteger(row.getCell(3));
                    String puesto = getCellValueAsString(row.getCell(4));
                    Integer idIndicador = getCellValueAsInteger(row.getCell(5));
                    String indicador = getCellValueAsString(row.getCell(6));

                    String key = id + "-" + negocio;

                    // Obtener o crear catálogo
                    CatalogoRow catalogo = catalogoMap.computeIfAbsent(key, k ->
                            CatalogoRow.builder()
                                    .id(id)
                                    .negocio(negocio)
                                    .puestos(new ArrayList<>())
                                    .build()
                    );

                    // Buscar o crear puesto
                    PuestoRow puestoExistente = catalogo.getPuestos().stream()
                            .filter(p -> p.getIdPuesto().equals(idPuesto))
                            .findFirst()
                            .orElse(null);

                    if (puestoExistente == null) {
                        puestoExistente = PuestoRow.builder()
                                .idPuesto(idPuesto)
                                .idFuncion(idFuncion)
                                .puesto(puesto)
                                .indicadores(new ArrayList<>())
                                .build();
                        catalogo.getPuestos().add(puestoExistente);
                    }

                    // Agregar indicador
                    IndicadorRow indicadorRow = IndicadorRow.builder()
                            .idIndicador(idIndicador)
                            .indicador(indicador)
                            .build();
                    puestoExistente.getIndicadores().add(indicadorRow);

                } catch (Exception e) {
                    log.error("Error en fila {}: {}", i + 1, e.getMessage());
                }
            }

            catalogos.addAll(catalogoMap.values());
            log.info("✅ Procesados {} catálogos con estructura jerárquica", catalogos.size());

        } catch (IOException e) {
            log.error("❌ Error leyendo archivo Excel: {}", e.getMessage());
            throw new IOException("Error procesando archivo Excel: " + e.getMessage(), e);
        }

        return catalogos;
    }

    // ========== MÉTODOS AUXILIARES ==========

    private boolean validarDatosInteligencia(DatosInteligenciaRow dato, int fila) {
        // Validaciones críticas
        if (dato.getPkiPeriodo() == null || dato.getPkiPeriodo().trim().isEmpty()) {
            log.warn("Fila {}: PkiPeriodo vacío", fila + 1);
            return false;
        }

        if (dato.getPkiPuesto() == null || dato.getPkiPuesto() <= 0) {
            log.warn("Fila {}: PkiPuesto inválido ({})", fila + 1, dato.getPkiPuesto());
            return false;
        }

        if (dato.getPkiGrupoNegocio() == null || dato.getPkiGrupoNegocio() <= 0) {
            log.warn("Fila {}: PkiGrupoNegocio inválido", fila + 1);
            return false;
        }

        if (dato.getPkiConceptoDetalle() == null || dato.getPkiConceptoDetalle() <= 0) {
            log.warn("Fila {}: PkiConceptoDetalle inválido", fila + 1);
            return false;
        }

        if (dato.getFnValor() == null) {
            log.warn("Fila {}: FnValor vacío", fila + 1);
            return false;
        }

        // Validaciones opcionales con valores por defecto
        if (dato.getPkiSucursal() == null) dato.setPkiSucursal(0);
        if (dato.getPkiEmpleado() == null) dato.setPkiEmpleado(0);
        if (dato.getPkiCDGenerico() == null) dato.setPkiCDGenerico(0);
        if (dato.getPkiPais() == null) dato.setPkiPais(1);
        if (dato.getPkiCanal() == null) dato.setPkiCanal(0);
        if (dato.getFnDetalle1() == null) dato.setFnDetalle1(0.0);
        if (dato.getFnDetalle2() == null) dato.setFnDetalle2(0.0);
        if (dato.getPkcDetalle3() == null) dato.setPkcDetalle3(0);
        if (dato.getFnDetalle7() == null) dato.setFnDetalle7(0.0);

        return true;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        String value = "";
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue().toString();
                } else {
                    // Para números, verificar si es entero o decimal
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        value = String.valueOf((long) numValue);
                    } else {
                        value = String.valueOf(numValue);
                    }
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                try {
                    value = String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    value = cell.getStringCellValue();
                }
                break;
            default:
                value = "";
        }

        // IMPORTANTE: Hacer trim() para eliminar espacios en blanco extras
        // que vienen en los campos de MongoDB
        return value.trim();
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;  // Valor por defecto

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                String strValue = cell.getStringCellValue().trim();
                if (strValue.isEmpty()) return 0.0;
                try {
                    return Double.parseDouble(strValue);
                } catch (NumberFormatException e) {
                    log.warn("No se pudo convertir '{}' a Double, usando 0.0", strValue);
                    return 0.0;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    log.warn("Error evaluando fórmula, usando 0.0");
                    return 0.0;
                }
            case BLANK:
                return 0.0;
            default:
                return 0.0;
        }
    }
}