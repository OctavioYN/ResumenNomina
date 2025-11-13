

package com.resumen.nomina.application.service;

import com.resumen.nomina.application.repository.IndicadorPromedioRepository;
import com.resumen.nomina.application.util.DocumentHelper;
import com.resumen.nomina.infrastructure.repository.IndicadorCalculadoRepositoryService;
import com.resumen.nomina.domain.model.*;
        import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
        import java.util.stream.Collectors;

@Service
public class IndicadorPromedioService {

    private static final Logger logger = LoggerFactory.getLogger(IndicadorPromedioService.class);

    private final IndicadorPromedioRepository indicadorPromedioRepository;
    private final IndicadorCalculadoRepositoryService indicadorCalculadoRepository;

    @Autowired
    public IndicadorPromedioService(IndicadorPromedioRepository indicadorPromedioRepository,
                                    IndicadorCalculadoRepositoryService indicadorCalculadoRepository) {
        this.indicadorPromedioRepository = indicadorPromedioRepository;
        this.indicadorCalculadoRepository = indicadorCalculadoRepository;
    }

    /**
     * Obtiene gráfica histórica para un indicador específico
     */
    public GraficaIndicadorPromedioResponse obtenerGraficaIndicador(Integer negocio, Integer puesto,
                                                                    Integer conceptoDetalle,
                                                                    Integer anioDesde, String usuario) {
        logger.info("Obteniendo gráfica para Negocio={}, Puesto={}, Indicador={}, Desde={}",
                negocio, puesto, conceptoDetalle, anioDesde);

        try {
            // Validar parámetros
            validarParametros(negocio, puesto, conceptoDetalle, anioDesde);

            logger.info("EntroObteniendo gráfica para Negocio={}, Puesto={}, Indicador={}, Desde={}",
                    negocio, puesto, conceptoDetalle, anioDesde);
            // 1. Obtener estadísticas históricas
            Document estadisticasDoc = indicadorCalculadoRepository.calcularEstadisticasIndicador(
                    negocio, puesto, conceptoDetalle, anioDesde);

            if (estadisticasDoc == null) {
                throw new RuntimeException("No hay datos suficientes para calcular estadísticas");
            }

            // 2. Obtener datos históricos
            List<Document> datosHistoricos = indicadorCalculadoRepository.obtenerPromediosIndicadorHistorico(
                    negocio, puesto, conceptoDetalle, anioDesde);

            if (datosHistoricos.isEmpty()) {
                throw new RuntimeException("No hay datos históricos disponibles");
            }

            // 3. Mapear a DTOs
            EstadisticasCompensacion estadisticas = mapearEstadisticas(estadisticasDoc, anioDesde);
            LineasConfianza lineasConfianza = mapearLineasConfianza(estadisticasDoc);
            List<PuntoGraficaPromedio> puntosGrafica = mapearPuntosGrafica(datosHistoricos, estadisticasDoc);

            String periodoAnalisis = String.format("Negocio %d - Puesto %d - Indicador %d (%d - presente)",
                    negocio, puesto, conceptoDetalle, anioDesde);

            return new GraficaIndicadorPromedioResponse(
                    negocio, puesto, conceptoDetalle,
                    estadisticas, lineasConfianza, puntosGrafica, periodoAnalisis
            );

        } catch (Exception e) {
            logger.error("Error obteniendo gráfica de indicador: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener gráfica de indicador: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene comparación semana actual vs anterior para un indicador
     */
    public IndicadorPromedioResponse obtenerIndicadorPromedioActual(Integer negocio, Integer puesto,
                                                                    Integer conceptoDetalle, String usuario) {
        logger.info("Obteniendo promedio actual para Negocio={}, Puesto={}, Indicador={}",
                negocio, puesto, conceptoDetalle);

        try {
            validarParametros(negocio, puesto, conceptoDetalle, 2023);

            // Obtener las últimas 2 semanas calculadas
            List<IndicadorPromedio> ultimasSemanas = indicadorPromedioRepository
                    .findByNegocioPuestoIndicador(negocio, puesto, conceptoDetalle)
                    .stream()
                    .sorted((a, b) -> {
                        int anioComp = b.getAnio().compareTo(a.getAnio());
                        return anioComp != 0 ? anioComp : b.getNumeroSemana().compareTo(a.getNumeroSemana());
                    })
                    .limit(2)
                    .collect(Collectors.toList());

            if (ultimasSemanas.size() >= 2) {
                return mapearAIndicadorPromedioResponse(ultimasSemanas.get(0), ultimasSemanas.get(1));
            } else if (ultimasSemanas.size() == 1) {
                IndicadorPromedio unica = ultimasSemanas.get(0);
                return new IndicadorPromedioResponse(
                        unica.getNegocio(), unica.getPuesto(), unica.getConceptoDetalle(),
                        unica.getSemana(), "N/A",
                        unica.getPromedio(), 0.0, unica.getPromedio(), null
                );
            }

            throw new RuntimeException("No hay datos calculados para este indicador");

        } catch (Exception e) {
            logger.error("Error obteniendo indicador promedio actual: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener indicador promedio actual: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa y guarda semanas faltantes para un indicador específico
     */
    @Transactional
    public int procesarSemanasFaltantesIndicador(Integer negocio, Integer puesto,
                                                 Integer conceptoDetalle, String usuario) {
        logger.info("Procesando semanas faltantes para Negocio={}, Puesto={}, Indicador={}",
                negocio, puesto, conceptoDetalle);

        try {
            validarParametros(negocio, puesto, conceptoDetalle, 2023);

            // 1. Obtener datos históricos completos desde 2023
            List<Document> datosHistoricos = indicadorCalculadoRepository.obtenerPromediosIndicadorHistorico(
                    negocio, puesto, conceptoDetalle, 2023);

            if (datosHistoricos.isEmpty()) {
                logger.warn("No hay datos históricos para procesar");
                return 0;
            }

            // 2. Obtener semanas ya procesadas
            List<IndicadorPromedio> semanasExistentes = indicadorPromedioRepository
                    .findByNegocioPuestoIndicador(negocio, puesto, conceptoDetalle);

            Set<String> semanasYaProcesadas = semanasExistentes.stream()
                    .map(IndicadorPromedio::getSemana)
                    .collect(Collectors.toSet());

            // 3. Identificar y procesar semanas nuevas
            int semanasProcessadas = 0;
            for (Document dato : datosHistoricos) {
                String semana = dato.getString("semana");

                if (!semanasYaProcesadas.contains(semana)) {
                    procesarSemanaIndividualIndicador(negocio, puesto, conceptoDetalle, dato, usuario);
                    semanasProcessadas++;
                }
            }

            // 4. Actualizar comparaciones y estadísticas
            if (semanasProcessadas > 0) {
                actualizarComparacionesYEstadisticasIndicador(negocio, puesto, conceptoDetalle, usuario);
            }

            logger.info("Procesamiento completado: {} semanas nuevas", semanasProcessadas);
            return semanasProcessadas;

        } catch (Exception e) {
            logger.error("Error procesando semanas faltantes: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando semanas faltantes: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todas las combinaciones disponibles de indicadores
     */
    public List<Map<String, Object>> listarIndicadoresDisponibles() {
        logger.info("Listando indicadores disponibles");

        try {
            List<Document> combinaciones = indicadorCalculadoRepository.listarCombinacionesDisponibles();

            return combinaciones.stream()
                    .map(doc -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("negocio", doc.getInteger("negocio"));
                        item.put("puesto", doc.getInteger("puesto"));
                        item.put("conceptoDetalle", doc.getInteger("conceptoDetalle"));
                        item.put("totalRegistros", doc.getInteger("totalRegistros"));
                        return item;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error listando indicadores: {}", e.getMessage(), e);
            throw new RuntimeException("Error al listar indicadores: " + e.getMessage(), e);
        }
    }

    // Métodos privados auxiliares

    private void procesarSemanaIndividualIndicador(Integer negocio, Integer puesto,
                                                   Integer conceptoDetalle,
                                                   Document dato, String usuario) {
        logger.debug("Procesando semana individual: {}", dato.getString("semana"));

        try {
            String semana = dato.getString("semana");
            Integer anio = dato.getInteger("anio");
            Integer numeroSemana = dato.getInteger("numeroSemana");
            Double totalIndicador = dato.getDouble("totalIndicador");
            Integer totalEmpleados = dato.getInteger("totalEmpleados");
            Double promedio = dato.getDouble("promedio");

            IndicadorPromedio indicador = new IndicadorPromedio(
                    negocio, puesto, conceptoDetalle,
                    semana, anio, numeroSemana,
                    totalIndicador, totalEmpleados, promedio,
                    1, usuario
            );

            // Guardar o reemplazar
            if (indicadorPromedioRepository.existsByNegocioAndPuestoAndConceptoDetalleAndSemana(
                    negocio, puesto, conceptoDetalle, semana)) {
                indicadorPromedioRepository.deleteByNegocioAndPuestoAndConceptoDetalleAndSemana(
                        negocio, puesto, conceptoDetalle, semana);
            }
            indicadorPromedioRepository.save(indicador);

            logger.debug("Semana {} guardada con promedio: {}", semana, promedio);

        } catch (Exception e) {
            logger.error("Error procesando semana individual: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando semana: " + e.getMessage(), e);
        }
    }

    private void actualizarComparacionesYEstadisticasIndicador(Integer negocio, Integer puesto,
                                                               Integer conceptoDetalle, String usuario) {
        logger.info("Actualizando comparaciones y estadísticas");

        try {
            // 1. Obtener todas las semanas ordenadas
            List<IndicadorPromedio> todasLasSemanas = indicadorPromedioRepository
                    .findByNegocioPuestoIndicador(negocio, puesto, conceptoDetalle);

            todasLasSemanas.sort((a, b) -> {
                int anioComp = a.getAnio().compareTo(b.getAnio());
                return anioComp != 0 ? anioComp : a.getNumeroSemana().compareTo(b.getNumeroSemana());
            });

            // 2. Calcular estadísticas históricas
            Document estadisticas = indicadorCalculadoRepository.calcularEstadisticasIndicador(
                    negocio, puesto, conceptoDetalle, 2023);

            Double mediaHistorica = null;
            Double desviacionEstandar = null;
            Double superior1DS = null;
            Double inferior1DS = null;
            Double superior15DS = null;
            Double inferior15DS = null;

            if (estadisticas != null) {
                mediaHistorica = estadisticas.getDouble("media");
                desviacionEstandar = estadisticas.getDouble("desviacionEstandar");
                superior1DS = estadisticas.getDouble("superior1DS");
                inferior1DS = estadisticas.getDouble("inferior1DS");
                superior15DS = estadisticas.getDouble("superior15DS");
                inferior15DS = estadisticas.getDouble("inferior15DS");
            }

            // 3. Actualizar cada registro
            List<IndicadorPromedio> semanasActualizadas = new ArrayList<>();

            for (int i = 0; i < todasLasSemanas.size(); i++) {
                IndicadorPromedio semanaActual = todasLasSemanas.get(i);

                semanaActual.setEsUltimaSemana(false);
                semanaActual.setMediaHistorica(mediaHistorica);
                semanaActual.setDesviacionEstandar(desviacionEstandar);
                semanaActual.setLineaSuperior1DS(superior1DS);
                semanaActual.setLineaInferior1DS(inferior1DS);
                semanaActual.setLineaSuperior15DS(superior15DS);
                semanaActual.setLineaInferior15DS(inferior15DS);

                if (i > 0) {
                    IndicadorPromedio semanaAnterior = todasLasSemanas.get(i - 1);

                    semanaActual.setSemanaAnterior(semanaAnterior.getSemana());
                    semanaActual.setPromedioSemanaAnterior(semanaAnterior.getPromedio());

                    Double diferencia = semanaActual.getPromedio() - semanaAnterior.getPromedio();
                    semanaActual.setDiferencia(diferencia);

                    if (semanaAnterior.getPromedio() != 0) {
                        Double variacion = ((semanaActual.getPromedio() / semanaAnterior.getPromedio()) - 1) * 100;
                        semanaActual.setVariacionPorcentual(Math.round(variacion * 10.0) / 10.0);
                    }
                } else {
                    semanaActual.setSemanaAnterior(null);
                    semanaActual.setPromedioSemanaAnterior(null);
                    semanaActual.setDiferencia(null);
                    semanaActual.setVariacionPorcentual(null);
                }

                semanasActualizadas.add(semanaActual);
            }

            // 4. Marcar última semana
            if (!semanasActualizadas.isEmpty()) {
                semanasActualizadas.get(semanasActualizadas.size() - 1).setEsUltimaSemana(true);
            }

            // 5. Guardar actualizaciones
            indicadorPromedioRepository.saveAll(semanasActualizadas);

            logger.info("Comparaciones actualizadas para {} semanas", semanasActualizadas.size());

        } catch (Exception e) {
            logger.error("Error actualizando comparaciones: {}", e.getMessage(), e);
            throw new RuntimeException("Error actualizando comparaciones: " + e.getMessage(), e);
        }
    }

    private void validarParametros(Integer negocio, Integer puesto, Integer conceptoDetalle, Integer anioDesde) {
        if (negocio == null || negocio <= 0) {
            throw new IllegalArgumentException("El negocio debe ser un número válido mayor a 0");
        }
        if (puesto == null || puesto <= 0) {
            throw new IllegalArgumentException("El puesto debe ser un número válido mayor a 0");
        }
        if (conceptoDetalle == null || conceptoDetalle <= 0 || conceptoDetalle == 101) {
            throw new IllegalArgumentException("El conceptoDetalle debe ser válido y diferente de 1011");
        }
        if (anioDesde == null || anioDesde < 2020 || anioDesde > 2070) {
            throw new IllegalArgumentException("El año debe estar entre 2020 y 2070");
        }
    }

    private EstadisticasCompensacion mapearEstadisticas(Document doc, Integer anioDesde) {
        return new EstadisticasCompensacion(
                DocumentHelper.getDoubleValue(doc, "media").longValue(),  // ✅
                DocumentHelper.getDoubleValue(doc, "desviacionEstandar").longValue(),  // ✅
                DocumentHelper.getIntegerValue(doc, "totalSemanas"),  // ✅
                anioDesde + " - presente"
        );
    }

    private LineasConfianza mapearLineasConfianza(Document doc) {
        return new LineasConfianza(
                DocumentHelper.getDoubleValue(doc, "media").longValue(),  // ✅
                DocumentHelper.getDoubleValue(doc, "superior1DS").longValue(),  // ✅
                DocumentHelper.getDoubleValue(doc, "inferior1DS").longValue(),  // ✅
                DocumentHelper.getDoubleValue(doc, "superior15DS").longValue(),  // ✅
                DocumentHelper.getDoubleValue(doc, "inferior15DS").longValue()  // ✅
        );
    }

    private List<PuntoGraficaPromedio> mapearPuntosGrafica(List<Document> datos, Document estadisticas) {
        List<PuntoGraficaPromedio> puntos = new ArrayList<>();

        Double media = DocumentHelper.getDoubleValue(estadisticas, "media");  // ✅
        Double superior1DS = DocumentHelper.getDoubleValue(estadisticas, "superior1DS");  // ✅
        Double inferior1DS = DocumentHelper.getDoubleValue(estadisticas, "inferior1DS");  // ✅
        Double superior15DS = DocumentHelper.getDoubleValue(estadisticas, "superior15DS");  // ✅
        Double inferior15DS = DocumentHelper.getDoubleValue(estadisticas, "inferior15DS");  // ✅

        for (int i = 0; i < datos.size(); i++) {
            Document punto = datos.get(i);

            Double variacionVsSA = 0.0;;
            if (i > 0) {
                Document anterior = datos.get(i - 1);
                Double promedioActual = DocumentHelper.getDoubleValue(punto, "promedio");  // ✅
                Double promedioAnterior = DocumentHelper.getDoubleValue(anterior, "promedio");  // ✅

                if (promedioAnterior != null && promedioAnterior != 0) {
                    variacionVsSA = ((promedioActual / promedioAnterior) - 1) * 100;
                    variacionVsSA = Math.round(variacionVsSA * 100.0) / 100.0;
                }
            }

            PuntoGraficaPromedio puntoGrafica = new PuntoGraficaPromedio(
                    DocumentHelper.getStringValue(punto, "semana"),  // ✅
                    DocumentHelper.getDoubleValue(punto, "promedio"),  // ✅
                    variacionVsSA,
                    media, superior1DS, inferior1DS, superior15DS, inferior15DS
            );

            puntos.add(puntoGrafica);
        }

        return puntos;
    }
    private IndicadorPromedioResponse mapearAIndicadorPromedioResponse(IndicadorPromedio actual,
                                                                       IndicadorPromedio anterior) {
        return new IndicadorPromedioResponse(
                actual.getNegocio(),
                actual.getPuesto(),
                actual.getConceptoDetalle(),
                actual.getSemana(),
                anterior.getSemana(),
                actual.getPromedio(),
                anterior.getPromedio(),
                actual.getDiferencia() != null ? actual.getDiferencia() : 0.0,
                actual.getVariacionPorcentual()
        );
    }


    

}