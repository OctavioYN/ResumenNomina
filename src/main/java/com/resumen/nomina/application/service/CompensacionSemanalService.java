package com.resumen.nomina.application.service;

import org.springframework.data.domain.PageRequest;
import com.resumen.nomina.application.repository.CompensacionSemanalRepository;
import com.resumen.nomina.infrastructure.repository.CompensacionSemanalInfrastructureRepository;
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
public class CompensacionSemanalService {

    private static final Logger logger = LoggerFactory.getLogger(CompensacionSemanalService.class);

    private final CompensacionSemanalRepository compensacionRepository;
    private final CompensacionSemanalInfrastructureRepository infraRepository;

    @Autowired
    public CompensacionSemanalService(CompensacionSemanalRepository compensacionRepository,
                                      CompensacionSemanalInfrastructureRepository infraRepository) {
        this.compensacionRepository = compensacionRepository;
        this.infraRepository = infraRepository;
    }

    /**
     * Obtiene compensación semanal actual vs anterior (para tabla)
     * Procesa automáticamente semanas faltantes antes de devolver resultado
     */
    public CompensacionSemanalResponse obtenerCompensacionSemanalActual(String usuario) {
        logger.info("Obteniendo compensación semanal actual por usuario: {}", usuario);

        try {
            // 1. Procesar semanas faltantes automáticamente
            procesarSemanasFaltantes(usuario);

            // 2. Obtener las últimas 2 semanas ya procesadas
            List<CompensacionSemanal> ultimasSemanas = compensacionRepository.findTop2ByOrderByAnioDescNumeroSemanaDesc();

            if (ultimasSemanas.size() >= 2) {
                logger.info("Devolviendo datos actualizados - Semana {} vs {}",
                        ultimasSemanas.get(0).getSemana(), ultimasSemanas.get(1).getSemana());
                return mapearACompensacionResponse(ultimasSemanas.get(0), ultimasSemanas.get(1));
            } else if (ultimasSemanas.size() == 1) {
                // Solo hay una semana, crear respuesta con valores en cero para la anterior
                CompensacionSemanal unica = ultimasSemanas.get(0);
                return new CompensacionSemanalResponse(
                        unica.getSemana(), "N/A",
                        unica.getTotalCompensacionNacional().longValue(), 0L,
                        unica.getTotalCompensacionNacional().longValue(), null
                );
            }

            throw new RuntimeException("No hay datos suficientes para generar respuesta");

        } catch (Exception e) {
            logger.error("Error obteniendo compensación semanal: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener compensación semanal: " + e.getMessage(), e);
        }
    }

    /**
     * Fuerza el recálculo y actualización de la compensación semanal actual
     */
    @Transactional
    public CompensacionSemanalResponse recalcularCompensacionSemanalActual(String usuario) {
        logger.info("Forzando recálculo de compensación semanal por usuario: {}", usuario);

        try {
            // 1. Eliminar flag de última semana de registros anteriores
            List<CompensacionSemanal> anteriores = compensacionRepository.findByEsUltimaSemanaTrue();
            anteriores.forEach(cs -> cs.setEsUltimaSemana(false));
            if (!anteriores.isEmpty()) {
                compensacionRepository.saveAll(anteriores);
            }

            // 2. Procesar semanas faltantes
            procesarSemanasFaltantes(usuario);

            // 3. Devolver resultado actualizado
            return obtenerCompensacionSemanalActual(usuario);

        } catch (Exception e) {
            logger.error("Error recalculando compensación semanal: {}", e.getMessage(), e);
            throw new RuntimeException("Error al recalcular compensación semanal: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa todas las semanas faltantes de manera incremental
     */
    @Transactional
    public int procesarSemanasFaltantes(String usuario) {
        logger.info("Iniciando procesamiento de semanas faltantes por usuario: {}", usuario);

        try {
            // 1. Obtener todas las semanas disponibles en IndicadoresCalculados
            List<String> semanasDisponibles = obtenerSemanasDisponibles();
            logger.info("Semanas disponibles en IndicadoresCalculados: {}", semanasDisponibles.size());

            // 2. Obtener semanas ya procesadas
            List<CompensacionSemanal> semanasExistentes = compensacionRepository.findAll();
            Set<String> semanasYaProcesadas = semanasExistentes.stream()
                    .map(CompensacionSemanal::getSemana)
                    .collect(Collectors.toSet());

            logger.info("Semanas ya procesadas: {}", semanasYaProcesadas.size());

            // 3. Identificar semanas faltantes
            List<String> semanasFaltantes = semanasDisponibles.stream()
                    .filter(semana -> !semanasYaProcesadas.contains(semana))
                    .sorted() // Procesar en orden cronológico
                    .collect(Collectors.toList());

            logger.info("Semanas faltantes a procesar: {}", semanasFaltantes);

            if (semanasFaltantes.isEmpty()) {
                logger.info("No hay semanas faltantes que procesar");
                return 0;
            }

            // 4. Procesar semanas faltantes una por una
            int semanasProcessadas = 0;
            for (String semana : semanasFaltantes) {
                try {
                    procesarSemanaIndividual(semana, usuario);
                    semanasProcessadas++;
                    logger.info("Semana {} procesada exitosamente ({}/{})",
                            semana, semanasProcessadas, semanasFaltantes.size());
                } catch (Exception e) {
                    logger.error("Error procesando semana {}: {}", semana, e.getMessage());
                    // Continuar con las demás semanas
                }
            }

            // 5. Recalcular comparaciones y estadísticas para todas las semanas
            if (semanasProcessadas > 0) {
                actualizarComparacionesYEstadisticas(usuario);
            }

            logger.info("Procesamiento completado: {} semanas nuevas procesadas", semanasProcessadas);
            return semanasProcessadas;

        } catch (Exception e) {
            logger.error("Error en procesamiento de semanas faltantes: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando semanas faltantes: " + e.getMessage(), e);
        }
    }

    /**
     * Fuerza el procesamiento completo de todas las semanas disponibles
     */
    @Transactional
    public int sincronizarTodasLasSemanas(String usuario) {
        logger.info("Iniciando sincronización completa de todas las semanas por usuario: {}", usuario);

        try {
            // 1. Limpiar datos existentes si se requiere resincronización completa
            // compensacionRepository.deleteAll(); // Descomenta si quieres limpiar todo

            // 2. Procesar todas las semanas faltantes
            return procesarSemanasFaltantes(usuario);

        } catch (Exception e) {
            logger.error("Error en sincronización completa: {}", e.getMessage(), e);
            throw new RuntimeException("Error en sincronización completa: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene datos históricos para gráfica desde un año específico
     */
    public GraficaCompensacionResponse obtenerGraficaHistorica(Integer anioDesde, String usuario) {
        logger.info("Obteniendo gráfica histórica desde año: {} por usuario: {}", anioDesde, usuario);

        try {
            // 1. Obtener estadísticas históricas
            Document estadisticasDoc = infraRepository.calcularEstadisticasHistoricas(anioDesde);
            if (estadisticasDoc == null) {
                throw new RuntimeException("No se pudieron calcular estadísticas históricas");
            }

            // 2. Obtener datos históricos
            List<Document> datosHistoricos = infraRepository.obtenerDatosHistoricosCompensacion(anioDesde);
            if (datosHistoricos.isEmpty()) {
                throw new RuntimeException("No hay datos históricos disponibles desde " + anioDesde);
            }

            // 3. Mapear a DTOs
            EstadisticasCompensacion estadisticas = mapearEstadisticas(estadisticasDoc, anioDesde);
            LineasConfianza lineasConfianza = mapearLineasConfianza(estadisticasDoc);
            List<PuntoGrafica> puntosGrafica = mapearPuntosGrafica(datosHistoricos, estadisticasDoc);

            String periodoAnalisis = anioDesde + " - presente";

            return new GraficaCompensacionResponse(estadisticas, lineasConfianza, puntosGrafica, periodoAnalisis);

        } catch (Exception e) {
            logger.error("Error obteniendo gráfica histórica: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener gráfica histórica: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene compensación de una semana específica
     */
    public Optional<CompensacionSemanal> obtenerCompensacionPorSemana(String semana) {
        logger.info("Buscando compensación para semana: {}", semana);
        return compensacionRepository.findBySemana(semana);
    }

    /**
     * Obtiene todas las compensaciones de un año específico
     */
    public List<CompensacionSemanal> obtenerCompensacionesPorAnio(Integer anio) {
        logger.info("Obteniendo compensaciones del año: {}", anio);
        return compensacionRepository.findByAnioOrderByNumeroSemanaAsc(anio);
    }

    /**
     * Obtiene estadísticas generales del sistema
     */
    public EstadisticasCompensacion obtenerEstadisticasGenerales(Integer anioDesde) {
        logger.info("Calculando estadísticas generales desde: {}", anioDesde);

        try {
            Document estadisticasDoc = infraRepository.calcularEstadisticasHistoricas(anioDesde);
            if (estadisticasDoc == null) {
                throw new RuntimeException("No se pudieron calcular estadísticas generales");
            }

            return mapearEstadisticas(estadisticasDoc, anioDesde);

        } catch (Exception e) {
            logger.error("Error calculando estadísticas generales: {}", e.getMessage(), e);
            throw new RuntimeException("Error al calcular estadísticas generales: " + e.getMessage(), e);
        }
    }

    // Métodos auxiliares para procesamiento incremental

    /**
     * Obtiene todas las semanas disponibles en IndicadoresCalculados
     */
    private List<String> obtenerSemanasDisponibles() {
        try {
            return infraRepository.obtenerSemanasDisponibles();
        } catch (Exception e) {
            logger.error("Error obteniendo semanas disponibles: {}", e.getMessage(), e);
            throw new RuntimeException("Error obteniendo semanas disponibles: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa una semana individual y la guarda en CompensacionesSemana
     */
    private void procesarSemanaIndividual(String semana, String usuario) {
        logger.debug("Procesando semana individual: {}", semana);

        try {
            // 1. Obtener datos de compensación para esta semana específica
            Document datosSemanales = infraRepository.calcularCompensacionSemanaIndividual(semana);
            if (datosSemanales == null) {
                logger.warn("No se encontraron datos para la semana: {}", semana);
                return;
            }

            // 2. Extraer información del documento
            Double totalCompensacion = datosSemanales.getDouble("totalCompensacion");
            Integer totalRegistros = datosSemanales.getInteger("totalRegistros");
            Integer cantidadNegocios = datosSemanales.getInteger("cantidadNegocios");
            Integer cantidadPuestos = datosSemanales.getInteger("cantidadPuestos");
            Integer anio = datosSemanales.getInteger("anio");
            Integer numeroSemana = datosSemanales.getInteger("numeroSemana");

            // 3. Crear registro de CompensacionSemanal
            CompensacionSemanal compensacion = new CompensacionSemanal();
            compensacion.setSemana(semana);
            compensacion.setAnio(anio);
            compensacion.setNumeroSemana(numeroSemana);
            compensacion.setTotalCompensacionNacional(totalCompensacion);
            compensacion.setTotalRegistros(totalRegistros);
            compensacion.setCantidadNegocios(cantidadNegocios);
            compensacion.setCantidadPuestos(cantidadPuestos);
            compensacion.setUsuarioCalculo(usuario);
            compensacion.setEsUltimaSemana(false); // Se actualizará después

            // 4. Guardar en BD (reemplazar si existe)
            if (compensacionRepository.existsBySemana(semana)) {
                compensacionRepository.deleteBySemana(semana);
            }
            compensacionRepository.save(compensacion);

            logger.debug("Semana {} guardada con total: ${}", semana, totalCompensacion.longValue());

        } catch (Exception e) {
            logger.error("Error procesando semana individual {}: {}", semana, e.getMessage(), e);
            throw new RuntimeException("Error procesando semana " + semana + ": " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza las comparaciones vs semana anterior y estadísticas históricas
     */
    private void actualizarComparacionesYEstadisticas(String usuario) {
        logger.info("Actualizando comparaciones y estadísticas históricas");

        try {
            // 1. Obtener todas las semanas ordenadas cronológicamente
            List<CompensacionSemanal> todasLasSemanas = compensacionRepository.findFromYear(2023);
            todasLasSemanas.sort((a, b) -> {
                int anioComp = a.getAnio().compareTo(b.getAnio());
                return anioComp != 0 ? anioComp : a.getNumeroSemana().compareTo(b.getNumeroSemana());
            });

            // 2. Calcular estadísticas históricas
            Document estadisticas = infraRepository.calcularEstadisticasHistoricas(2023);

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

            // 3. Actualizar cada registro con comparación vs anterior y estadísticas
            List<CompensacionSemanal> semanasActualizadas = new ArrayList<>();

            for (int i = 0; i < todasLasSemanas.size(); i++) {
                CompensacionSemanal semanaActual = todasLasSemanas.get(i);

                // Limpiar flag de última semana
                semanaActual.setEsUltimaSemana(false);

                // Agregar estadísticas históricas
                semanaActual.setMediaHistorica(mediaHistorica);
                semanaActual.setDesviacionEstandar(desviacionEstandar);
                semanaActual.setLineaSuperior1DS(superior1DS);
                semanaActual.setLineaInferior1DS(inferior1DS);
                semanaActual.setLineaSuperior15DS(superior15DS);
                semanaActual.setLineaInferior15DS(inferior15DS);

                // Calcular comparación con semana anterior
                if (i > 0) {
                    CompensacionSemanal semanaAnterior = todasLasSemanas.get(i - 1);

                    semanaActual.setSemanaAnterior(semanaAnterior.getSemana());
                    semanaActual.setTotalSemanaAnterior(semanaAnterior.getTotalCompensacionNacional());

                    // Calcular diferencia
                    Double diferencia = semanaActual.getTotalCompensacionNacional() - semanaAnterior.getTotalCompensacionNacional();
                    semanaActual.setDiferenciaPesos(diferencia);

                    // Calcular variación porcentual
                    if (semanaAnterior.getTotalCompensacionNacional() != 0) {
                        Double variacion = ((semanaActual.getTotalCompensacionNacional() / semanaAnterior.getTotalCompensacionNacional()) - 1) * 100;
                        semanaActual.setVariacionPorcentual(Math.round(variacion * 10.0) / 10.0); // 1 decimal
                    }
                } else {
                    // Primera semana, no hay comparación
                    semanaActual.setSemanaAnterior(null);
                    semanaActual.setTotalSemanaAnterior(null);
                    semanaActual.setDiferenciaPesos(null);
                    semanaActual.setVariacionPorcentual(null);
                }

                semanasActualizadas.add(semanaActual);
            }

            // 4. Marcar la última semana
            if (!semanasActualizadas.isEmpty()) {
                semanasActualizadas.get(semanasActualizadas.size() - 1).setEsUltimaSemana(true);
            }

            // 5. Guardar todas las actualizaciones
            compensacionRepository.saveAll(semanasActualizadas);

            logger.info("Comparaciones y estadísticas actualizadas para {} semanas", semanasActualizadas.size());

        } catch (Exception e) {
            logger.error("Error actualizando comparaciones: {}", e.getMessage(), e);
            throw new RuntimeException("Error actualizando comparaciones: " + e.getMessage(), e);
        }
    }

    // Métodos privados de mapeo

    private CompensacionSemanalResponse mapearACompensacionResponse(CompensacionSemanal actual, CompensacionSemanal anterior) {
        return new CompensacionSemanalResponse(
                actual.getSemana(),
                anterior.getSemana(),
                actual.getTotalCompensacionNacional().longValue(),
                anterior.getTotalCompensacionNacional().longValue(),
                actual.getDiferenciaPesos() != null ? actual.getDiferenciaPesos().longValue() : 0L,
                actual.getVariacionPorcentual()
        );
    }

    private EstadisticasCompensacion mapearEstadisticas(Document doc, Integer anioDesde) {
        return new EstadisticasCompensacion(
                doc.getDouble("media").longValue(),
                doc.getDouble("desviacionEstandar").longValue(),
                doc.getInteger("totalSemanas"),
                anioDesde + " - presente"
        );
    }

    private LineasConfianza mapearLineasConfianza(Document doc) {
        return new LineasConfianza(
                doc.getDouble("media").longValue(),
                doc.getDouble("superior1DS").longValue(),
                doc.getDouble("inferior1DS").longValue(),
                doc.getDouble("superior15DS").longValue(),
                doc.getDouble("inferior15DS").longValue()
        );
    }

    private List<PuntoGrafica> mapearPuntosGrafica(List<Document> datos, Document estadisticas) {
        List<PuntoGrafica> puntos = new ArrayList<>();

        Long media = estadisticas.getDouble("media").longValue();
        Long superior1DS = estadisticas.getDouble("superior1DS").longValue();
        Long inferior1DS = estadisticas.getDouble("inferior1DS").longValue();
        Long superior15DS = estadisticas.getDouble("superior15DS").longValue();
        Long inferior15DS = estadisticas.getDouble("inferior15DS").longValue();

        for (int i = 0; i < datos.size(); i++) {
            Document punto = datos.get(i);

            // Calcular variación vs semana anterior
            Double variacionVsSA = null;
            if (i > 0) {
                Document anterior = datos.get(i - 1);
                Double totalActual = punto.getDouble("total");
                Double totalAnterior = anterior.getDouble("total");

                if (totalAnterior != 0) {
                    variacionVsSA = ((totalActual / totalAnterior) - 1) * 100;
                    variacionVsSA = Math.round(variacionVsSA * 100.0) / 100.0; // 2 decimales
                }
            }

            PuntoGrafica puntoGrafica = new PuntoGrafica(
                    punto.getString("semana"),
                    punto.getDouble("total").longValue(),
                    variacionVsSA,
                    media, superior1DS, inferior1DS, superior15DS, inferior15DS
            );

            puntos.add(puntoGrafica);
        }

        return puntos;
    }

    /**
     * Obtiene datos históricos para gráfica filtrado por negocio específico
     */
    public GraficaCompensacionResponse obtenerGraficaHistoricaPorNegocio(Integer negocio, Integer anioDesde, String usuario) {
        logger.info("Obteniendo gráfica histórica para negocio: {} desde año: {} por usuario: {}", negocio, anioDesde, usuario);

        try {
            // Validar parámetros
            if (negocio == null || negocio <= 0) {
                throw new IllegalArgumentException("El negocio debe ser un número válido mayor a 0");
            }
            if (anioDesde == null || anioDesde < 2020 || anioDesde > 2070) {
                throw new IllegalArgumentException("El año debe estar entre 2020 y 2070");
            }

            // 1. Obtener estadísticas históricas para este negocio
            Document estadisticasDoc = infraRepository.calcularEstadisticasHistoricasPorNegocio(negocio, anioDesde);
            if (estadisticasDoc == null) {
                throw new RuntimeException("No se pudieron calcular estadísticas históricas para negocio " + negocio);
            }

            // 2. Obtener datos históricos para este negocio
            List<Document> datosHistoricos = infraRepository.obtenerDatosHistoricosCompensacionPorNegocio(negocio, anioDesde);
            if (datosHistoricos.isEmpty()) {
                throw new RuntimeException("No hay datos históricos disponibles para negocio " + negocio + " desde " + anioDesde);
            }

            // 3. Mapear a DTOs
            EstadisticasCompensacion estadisticas = mapearEstadisticas(estadisticasDoc, anioDesde);
            LineasConfianza lineasConfianza = mapearLineasConfianza(estadisticasDoc);
            List<PuntoGrafica> puntosGrafica = mapearPuntosGrafica(datosHistoricos, estadisticasDoc);

            String periodoAnalisis = "Negocio " + negocio + " (" + anioDesde + " - presente)";

            return new GraficaCompensacionResponse(estadisticas, lineasConfianza, puntosGrafica, periodoAnalisis);

        } catch (Exception e) {
            logger.error("Error obteniendo gráfica histórica por negocio: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener gráfica histórica por negocio: " + e.getMessage(), e);
        }
    }

    // Agregar este método al Service de Compensación



    /**
     * Obtiene compensación de una semana específica vs su semana anterior
     * @param semana formato YYYYWW (ejemplo: "202539")
     * @param usuario usuario que solicita
     */
    public CompensacionSemanalResponse obtenerCompensacionPorSemana(String semana, String usuario) {
        logger.info("Obteniendo compensación para semana específica: {} por usuario: {}", semana, usuario);

        try {
            // Validar formato de semana
            validarFormatoSemana(semana);

            // 1. Procesar semanas faltantes automáticamente (hasta la semana solicitada)
            procesarSemanasFaltantes(usuario);

            // 2. Obtener la semana solicitada y la anterior
            List<CompensacionSemanal> semanas = compensacionRepository
                    .findTop2BySemanaLessThanEqualOrderByAnioDescNumeroSemanaDesc(
                            semana,
                            PageRequest.of(0, 2)
                    );

            if (semanas.isEmpty()) {
                throw new RuntimeException("No se encontró información para la semana " + semana);
            }

            // Verificar que la primera semana sea exactamente la solicitada
            CompensacionSemanal semanaActual = semanas.get(0);
            if (!semanaActual.getSemana().equals(semana)) {
                throw new RuntimeException("La semana " + semana + " no existe en la base de datos. " +
                        "Semana más cercana encontrada: " + semanaActual.getSemana());
            }

            if (semanas.size() >= 2) {
                // Tenemos la semana solicitada y la anterior
                logger.info("Devolviendo datos - Semana {} vs {}",
                        semanas.get(0).getSemana(), semanas.get(1).getSemana());
                return mapearACompensacionResponse(semanas.get(0), semanas.get(1));
            } else {
                // Solo existe la semana solicitada, no hay anterior
                logger.info("Solo existe la semana {}, no hay semana anterior", semana);
                return new CompensacionSemanalResponse(
                        semanaActual.getSemana(),
                        "N/A",
                        semanaActual.getTotalCompensacionNacional().longValue(),
                        0L,
                        semanaActual.getTotalCompensacionNacional().longValue(),
                        null
                );
            }

        } catch (Exception e) {
            logger.error("Error obteniendo compensación para semana {}: {}", semana, e.getMessage(), e);
            throw new RuntimeException("Error al obtener compensación para semana " + semana + ": " + e.getMessage(), e);
        }
    }

    /**
     * Valida que el formato de semana sea correcto (YYYYWW)
     */
    private void validarFormatoSemana(String semana) {
        if (semana == null || semana.length() != 6) {
            throw new IllegalArgumentException("Formato de semana inválido. Debe ser YYYYWW, ejemplo: 202539");
        }

        try {
            int anio = Integer.parseInt(semana.substring(0, 4));
            int numeroSemana = Integer.parseInt(semana.substring(4, 6));

            if (anio < 2020 || anio > 2070) {
                throw new IllegalArgumentException("Año debe estar entre 2020 y 2070");
            }

            if (numeroSemana < 1 || numeroSemana > 53) {
                throw new IllegalArgumentException("Número de semana debe estar entre 01 y 53");
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de semana inválido: " + semana);
        }
    }

}