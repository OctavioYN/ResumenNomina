package com.resumen.nomina.application.service;

import com.resumen.nomina.application.repository.ConfiguracionAlertasRepository;
import com.resumen.nomina.application.util.DocumentHelper;
import com.resumen.nomina.domain.model.*;
import com.resumen.nomina.infrastructure.repository.AlertasInfrastructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🚨 SERVICIO OPTIMIZADO DE ALERTAS CON CONFIGURACIÓN DINÁMICA
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertasServiceOptimizado {

    private final AlertasInfrastructureRepository alertasRepository;
    private final ConfiguracionAlertasRepository configuracionRepository;

    // ========== VALORES POR DEFECTO (FALLBACK) ==========
    private static final int DEFAULT_PERIODOS_MINIMOS = 12;
    private static final int DEFAULT_PERIODOS_ROBUSTO = 24;
    private static final double DEFAULT_UMBRAL_CRITICO = 2.5;
    private static final double DEFAULT_UMBRAL_ALTO = 1.96;
    private static final double DEFAULT_UMBRAL_MODERADO = 1.0;
    private static final double DEFAULT_NIVEL_CONFIANZA = 1.96;

    /**
     * 🔧 Carga configuración activa o crea una por defecto
     */
   /* private ConfiguracionAlertas obtenerConfiguracion() {
        return configuracionRepository
                .findByCodigoConfiguracionAndActivaTrue("DEFAULT")
                .orElseGet(() -> {
                    log.warn("⚠️ No se encontró configuración activa, usando valores por defecto");
                    ConfiguracionAlertas config = ConfiguracionAlertas.crearConfiguracionPorDefecto();

                    // Intentar guardar configuración por defecto
                    try {
                        configuracionRepository.save(config);
                        log.info("✅ Configuración por defecto creada y guardada");
                    } catch (Exception e) {
                        log.error("❌ Error guardando configuración por defecto: {}", e.getMessage());
                    }

                    return config;
                });
    }*/

    /**
     * 🔧 Carga configuración específica o usa DEFAULT como fallback
     */
    private ConfiguracionAlertas obtenerConfiguracion(String codigoConfig) {
        // Si no se especifica código, usar DEFAULT
        String codigo = (codigoConfig != null && !codigoConfig.trim().isEmpty())
                ? codigoConfig : "DEFAULT";

        return configuracionRepository
                .findByCodigoConfiguracionAndActivaTrue(codigo)
                .orElseGet(() -> {
                    log.warn("⚠️ No se encontró configuración '{}', intentando con DEFAULT", codigo);

                    // Intentar con DEFAULT como fallback
                    return configuracionRepository
                            .findByCodigoConfiguracionAndActivaTrue("DEFAULT")
                            .orElseGet(() -> {
                                log.warn("⚠️ No existe DEFAULT, creando configuración por defecto");
                                ConfiguracionAlertas config = ConfiguracionAlertas.crearConfiguracionPorDefecto();

                                try {
                                    configuracionRepository.save(config);
                                    log.info("✅ Configuración DEFAULT creada");
                                } catch (Exception e) {
                                    log.error("❌ Error guardando configuración: {}", e.getMessage());
                                }

                                return config;
                            });
                });
    }


    /**
     * 📊 OBTENER ALERTAS Z-SCORE (con configuración dinámica)
     */
    public AlertasZScoreResponse obtenerAlertasZScore(
            String periodoActual,
            String sucursal,
            Integer negocio,
            String codigoConfiguracion) {

        log.info("📊 Generando alertas Z-Score - Período: {}, Config: {}",
                periodoActual, codigoConfiguracion);

        try {

            // 1. Cargar configuración específica
            ConfiguracionAlertas config = obtenerConfiguracion(codigoConfiguracion);
            log.info("🔧 Usando configuración: {} - Períodos mínimos: {}, Umbral crítico: {}",
                    config.getCodigoConfiguracion(),
                    config.getPeriodosMinimosHistoricos(),
                    config.getUmbralCritico());

            // 2. Obtener datos crudos del repository

            List<Document> docs = alertasRepository.calcularAlertasZScore(
                    periodoActual, sucursal, negocio, config);



            if (docs.isEmpty()) {
                log.warn("⚠️ No hay datos para Z-Score en período: {}", periodoActual);
                return crearRespuestaZScoreVacia(periodoActual);
            }

            // 3. Mapear a objetos de dominio con configuración dinámica
            List<AlertaZScoreDTO> alertas = docs.stream()
                    .map(doc -> mapearAlertaZScoreOptimizada(doc, config))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingDouble(AlertaZScoreDTO::getZScoreAbsoluto).reversed())
                    .collect(Collectors.toList());

            // 4. Generar resumen
            ResumenZScore resumen = generarResumenZScore(alertas);

            // 5. Construir respuesta final
            return AlertasZScoreResponse.builder()
                    .tipo("Z_SCORE")
                    .titulo("Z-Score")
                    .descripcion("Se activa una alerta cuando la variación semanal (vs. semana anterior) " +
                            "sale del rango habitual, considerando:")
                    .consideraciones(Arrays.asList(
                            "El comportamiento histórico del indicador (media y variabilidad típica)",
                            "La severidad del cambio (umbrales configurables)"
                    ))
                    .periodoActual(periodoActual)
                    .sucursal(sucursal != null ? sucursal : "TODAS")
                    .resumen(resumen)
                    .alertas(alertas)
                    .fechaGeneracion(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error generando alertas Z-Score: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando alertas Z-Score", e);
        }
    }

    /**
     * 📈 OBTENER ALERTAS ARIMA (con configuración dinámica)
     */
    public AlertasARIMAResponse obtenerAlertasARIMA(
            String periodoActual,
            String sucursal,
            Integer negocio,
            String codigoConfiguracion) {

        log.info("📈 Generando alertas ARIMA - Período: {}, Config: {}",
                periodoActual, codigoConfiguracion);

        try {
            // 1. Cargar configuración
            ConfiguracionAlertas config = obtenerConfiguracion(codigoConfiguracion);


            // 2. Obtener datos crudos
            List<Document> docs = alertasRepository.calcularAlertasARIMA(
                    periodoActual, sucursal, negocio, config);

            if (docs.isEmpty()) {
                log.warn("⚠️ No hay datos para ARIMA en período: {}", periodoActual);
                return crearRespuestaARIMAVacia(periodoActual);
            }

            // 3. Mapear y filtrar por historia suficiente
            List<AlertaARIMADTO> alertas = docs.stream()
                    .map(doc -> mapearAlertaARIMAOptimizada(doc, config))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Separar modelos robustos vs no robustos
            List<AlertaARIMADTO> modelosRobustos = alertas.stream()
                    .filter(AlertaARIMADTO::isModeloRobusto)
                    .collect(Collectors.toList());

            List<AlertaARIMADTO> modelosNoRobustos = alertas.stream()
                    .filter(a -> !a.isModeloRobusto())
                    .collect(Collectors.toList());

            // 4. Generar resumen
            ResumenARIMA resumen = generarResumenARIMA(alertas, modelosRobustos, modelosNoRobustos);

            // 5. Construir respuesta final
            return AlertasARIMAResponse.builder()
                    .tipo("ARIMA")
                    .titulo("Intervalo de Predicción 95%")
                    .descripcion("Se activa una alerta cuando el valor observado está fuera del " +
                            "intervalo de predicción del 95% construido con el modelo ARIMA (p,d,q)")
                    .advertencia("*Puestos sin suficiente historia. Por lo tanto, este modelo no es robusto en ellos.")
                    .periodoActual(periodoActual)
                    .sucursal(sucursal != null ? sucursal : "TODAS")
                    .resumen(resumen)
                    .alertas(alertas)
                    .modelosRobustos(modelosRobustos)
                    .modelosNoRobustos(modelosNoRobustos)
                    .fechaGeneracion(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error generando alertas ARIMA: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando alertas ARIMA", e);
        }
    }

    /**
     * 🔄 OBTENER AMBAS ALERTAS (Completo)
     */
   /* @Transactional
    public AlertasCompletasResponse obtenerAlertasCompletas(
            String periodoActual,
            String sucursal,
            Integer negocio,
            boolean guardarHistorial,
            String usuario) {

        log.info("🔍 Generando alertas completas - Período: {}", periodoActual);

        AlertasZScoreResponse zscore = obtenerAlertasZScore(periodoActual, sucursal, negocio);
        AlertasARIMAResponse arima = obtenerAlertasARIMA(periodoActual, sucursal, negocio);

        // Opcional: Guardar en historial
        if (guardarHistorial) {
            guardarEnHistorial(zscore, arima, usuario);
        }

        return AlertasCompletasResponse.builder()
                .periodoActual(periodoActual)
                .sucursal(sucursal != null ? sucursal : "TODAS")
                .zscoreResponse(zscore)
                .arimaResponse(arima)
                .resumenGeneral(generarResumenGeneral(zscore, arima))
                .fechaGeneracion(LocalDateTime.now())
                .build();
    }*/



    /**
     * 🔄 OBTENER AMBAS ALERTAS (con configuración)
     */
    @Transactional
    public AlertasCompletasResponse obtenerAlertasCompletas(
            String periodoActual,
            String sucursal,
            Integer negocio,
            String codigoConfiguracion,  // ← NUEVO PARÁMETRO
            boolean guardarHistorial,
            String usuario) {

        log.info("🔍 Generando alertas completas - Período: {}, Config: {}",
                periodoActual, codigoConfiguracion);

        AlertasZScoreResponse zscore = obtenerAlertasZScore(
                periodoActual, sucursal, negocio, codigoConfiguracion);

        AlertasARIMAResponse arima = obtenerAlertasARIMA(
                periodoActual, sucursal, negocio, codigoConfiguracion);

        if (guardarHistorial) {
            guardarEnHistorial(zscore, arima, usuario);
        }

        return AlertasCompletasResponse.builder()
                .periodoActual(periodoActual)
                .sucursal(sucursal != null ? sucursal : "TODAS")
                .zscoreResponse(zscore)
                .arimaResponse(arima)
                .resumenGeneral(generarResumenGeneral(zscore, arima))
                .fechaGeneracion(LocalDateTime.now())
                .build();
    }

    // ========== MAPPERS OPTIMIZADOS CON CONFIGURACIÓN ==========

    /**
     * Mapea AlertaZScoreDTO según estructura de Imagen 1 (con configuración)
     */
    private AlertaZScoreDTO mapearAlertaZScoreOptimizada(Document doc, ConfiguracionAlertas config) {
        try {
            String puesto = DocumentHelper.getStringValue(doc, "puesto");
            String indicador = DocumentHelper.getStringValue(doc, "indicador");

            // Datos principales
            Double variacionVsSA = DocumentHelper.getDoubleValue(doc, "variacionVsSA");
            Double variacionMedia = DocumentHelper.getDoubleValue(doc, "variacionMedia");
            Double desviacionEstandar = DocumentHelper.getDoubleValue(doc, "desviacionEstandar");
            Double limiteInferior = DocumentHelper.getDoubleValue(doc, "limiteInferior");
            Double limiteSuperior = DocumentHelper.getDoubleValue(doc, "limiteSuperior");
            Double zScore = DocumentHelper.getDoubleValue(doc, "zScore");

            // Severidad usando configuración dinámica

            // String severidadStr = DocumentHelper.getStringValue(doc, "severidad");
            //SeveridadAlerta severidad = SeveridadAlerta.valueOf(severidadStr);

            SeveridadAlerta severidad = SeveridadAlerta.fromZScore(
                    zScore,
                    config.getUmbralCritico(),      // Umbral CRÍTICA desde BD
                    config.getUmbralAlto(),         // Umbral ALTA desde BD
                    config.getUmbralModerado()      // Umbral MODERADA desde BD
            );

            log.debug("🔍 Z-Score: {}, Umbral crítico: {} → Severidad: {}",
                    zScore, config.getUmbralCritico(), severidad);


            // Validar que tenga datos mínimos (usando configuración)
            Integer cantidadPeriodos = DocumentHelper.getIntegerValue(doc, "cantidadPeriodosHistoricos");
            if (cantidadPeriodos < config.getPeriodosMinimosHistoricos()) {
                log.debug("⚠️ Ignorando {} - {} (solo {} períodos, mínimo: {})",
                        puesto, indicador, cantidadPeriodos, config.getPeriodosMinimosHistoricos());
                return null;
            }

            return AlertaZScoreDTO.builder()
                    .puesto(puesto)
                    .indicador(indicador)
                    .conceptoDetalle(DocumentHelper.getIntegerValue(doc, "conceptoDetalle"))
                    .sucursal(DocumentHelper.getStringValue(doc, "sucursal"))
                    .negocio(DocumentHelper.getIntegerValue(doc, "negocio"))
                    .variacionPorcentualVsSA(redondear(variacionVsSA, 2))
                    .variacionMedia(redondear(variacionMedia, 2))
                    .desviacionEstandar(redondear(desviacionEstandar, 2))
                    .limiteInferior(redondear(limiteInferior, 2))
                    .limiteSuperior(redondear(limiteSuperior, 2))
                    .zScore(redondear(zScore, 2))
                    .zScoreAbsoluto(Math.abs(zScore))
                    .severidad(severidad)
                    .colorSeveridad(severidad.getColorHex())
                    .fueraDeRango(DocumentHelper.getBooleanValue(doc, "fueraDeRango"))
                    .cantidadPeriodosHistoricos(cantidadPeriodos)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapeando Z-Score: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mapea AlertaARIMA según estructura de Imagen 2 (con configuración)
     */
    private AlertaARIMADTO mapearAlertaARIMAOptimizada(Document doc, ConfiguracionAlertas config) {
        try {
            String puesto = DocumentHelper.getStringValue(doc, "puesto");
            String indicador = DocumentHelper.getStringValue(doc, "indicador");

            Double observacionReal = DocumentHelper.getDoubleValue(doc, "observacionReal");
            Double limiteInferior = DocumentHelper.getDoubleValue(doc, "limiteInferior");
            Double limiteSuperior = DocumentHelper.getDoubleValue(doc, "limiteSuperior");
            Double variacionFueraRango = DocumentHelper.getDoubleValue(doc, "variacionFueraDelRango");

            Integer cantidadPeriodos = DocumentHelper.getIntegerValue(doc, "cantidadPeriodosHistoricos");

            // Usar configuración para determinar si es robusto
            boolean modeloRobusto = cantidadPeriodos >= config.getPeriodosModeloRobusto();

            return AlertaARIMADTO.builder()
                    .puesto(puesto)
                    .indicador(indicador)
                    .conceptoDetalle(DocumentHelper.getIntegerValue(doc, "conceptoDetalle"))
                    .sucursal(DocumentHelper.getStringValue(doc, "sucursal"))
                    .negocio(DocumentHelper.getIntegerValue(doc, "negocio"))
                    .observacionReal(redondear(observacionReal, 2))
                    .limiteInferior(redondear(limiteInferior, 2))
                    .limiteSuperior(redondear(limiteSuperior, 2))
                    .rangoPrediccion(redondear(limiteSuperior - limiteInferior, 2))
                    .variacionFueraDelRango(redondear(Math.abs(variacionFueraRango), 0))
                    .direccionDesviacion(DocumentHelper.getStringValue(doc, "direccionDesviacion"))
                    .fueraDeRango(DocumentHelper.getBooleanValue(doc, "fueraDeRango"))
                    .cantidadPeriodosHistoricos(cantidadPeriodos)
                    .modeloRobusto(modeloRobusto)
                    .advertencia(modeloRobusto ? null : "*Sin historia suficiente")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapeando ARIMA: {}", e.getMessage());
            return null;
        }
    }

    // ========== GENERADORES DE RESUMEN (sin cambios) ==========

    private ResumenZScore generarResumenZScore(List<AlertaZScoreDTO> alertas) {
        Map<SeveridadAlerta, Long> porSeveridad = alertas.stream()
                .collect(Collectors.groupingBy(AlertaZScoreDTO::getSeveridad, Collectors.counting()));

        long fueraDeRango = alertas.stream().filter(AlertaZScoreDTO::isFueraDeRango).count();

        return ResumenZScore.builder()
                .totalIndicadoresEvaluados(alertas.size())
                .alertasCriticas(porSeveridad.getOrDefault(SeveridadAlerta.CRITICA, 0L).intValue())
                .alertasAltas(porSeveridad.getOrDefault(SeveridadAlerta.ALTA, 0L).intValue())
                .alertasModeradas(porSeveridad.getOrDefault(SeveridadAlerta.MODERADA, 0L).intValue())
                .alertasNormales(porSeveridad.getOrDefault(SeveridadAlerta.NORMAL, 0L).intValue())
                .indicadoresFueraDeRango((int) fueraDeRango)
                .porcentajeFueraDeRango(alertas.isEmpty() ? 0.0 :
                        redondear((fueraDeRango * 100.0) / alertas.size(), 1))
                .build();
    }

    private ResumenARIMA generarResumenARIMA(
            List<AlertaARIMADTO> todas,
            List<AlertaARIMADTO> robustas,
            List<AlertaARIMADTO> noRobustas) {

        long fueraDeRango = todas.stream().filter(AlertaARIMADTO::isFueraDeRango).count();

        return ResumenARIMA.builder()
                .totalIndicadoresEvaluados(todas.size())
                .modelosRobustos(robustas.size())
                .modelosNoRobustos(noRobustas.size())
                .indicadoresFueraDeRango((int) fueraDeRango)
                .porcentajeFueraDeRango(todas.isEmpty() ? 0.0 :
                        redondear((fueraDeRango * 100.0) / todas.size(), 1))
                .build();
    }

    private ResumenGeneral generarResumenGeneral(
            AlertasZScoreResponse zscore,
            AlertasARIMAResponse arima) {

        return ResumenGeneral.builder()
                .totalIndicadores(zscore.getAlertas().size() + arima.getAlertas().size())
                .alertasCriticasZScore(zscore.getResumen().getAlertasCriticas())
                .alertasFueraRangoARIMA(arima.getResumen().getIndicadoresFueraDeRango())
                .porcentajeAlertasActivas(calcularPorcentajeAlertasActivas(zscore, arima))
                .build();
    }

    // ========== UTILIDADES ==========

    private double redondear(Double valor, int decimales) {
        if (valor == null) return 0.0;
        double multiplicador = Math.pow(10, decimales);
        return Math.round(valor * multiplicador) / multiplicador;
    }

    private double calcularPorcentajeAlertasActivas(
            AlertasZScoreResponse zscore,
            AlertasARIMAResponse arima) {

        int total = zscore.getAlertas().size() + arima.getAlertas().size();
        if (total == 0) return 0.0;

        int activas = zscore.getResumen().getIndicadoresFueraDeRango() +
                arima.getResumen().getIndicadoresFueraDeRango();

        return redondear((activas * 100.0) / total, 1);
    }

    private AlertasZScoreResponse crearRespuestaZScoreVacia(String periodo) {
        return AlertasZScoreResponse.builder()
                .tipo("Z_SCORE")
                .periodoActual(periodo)
                .alertas(new ArrayList<>())
                .resumen(ResumenZScore.builder().totalIndicadoresEvaluados(0).build())
                .build();
    }

    private AlertasARIMAResponse crearRespuestaARIMAVacia(String periodo) {
        return AlertasARIMAResponse.builder()
                .tipo("ARIMA")
                .periodoActual(periodo)
                .alertas(new ArrayList<>())
                .resumen(ResumenARIMA.builder().totalIndicadoresEvaluados(0).build())
                .build();
    }

    private void guardarEnHistorial(
            AlertasZScoreResponse zscore,
            AlertasARIMAResponse arima,
            String usuario) {
        // Implementar guardado en colección AlertasGeneradas si es necesario
        log.info("💾 Guardando alertas en historial para usuario: {}", usuario);
    }
}