package com.resumen.nomina.alertas.arima.application.service;

import com.resumen.nomina.alertas.arima.domain.model.*;
        import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.springframework.stereotype.Service;

import java.util.*;
        import java.util.stream.Collectors;

/**
 * 🧮 SERVICIO DE MODELADO ARIMA
 *
 * Implementación de ARIMA usando Apache Commons Math
 * Basado en el documento PDF: "Alertas de compensación - Modelo ARIMA"
 */
@Slf4j
@Service
public class ArimaModelService {

    /**
     * 🔴 MÉTODO PRINCIPAL: Auto-ARIMA
     *
     * Implementa una versión simplificada de auto.arima() de R:
     * 1. Determina orden de diferenciación (d) automáticamente
     * 2. Prueba combinaciones de (p,q) según config
     * 3. Selecciona mejor modelo por AIC/BIC
     * 4. Genera pronóstico con intervalo del 95%
     */
    public ArimaForecast ajustarYPronosticar(List<Double> serie, ArimaConfig config) {

        log.debug("📊 Ajustando modelo ARIMA para serie de {} observaciones", serie.size());

        try {
            // 1. Validar serie
            if (serie == null || serie.size() < config.getPeriodosMinimos()) {
                throw new IllegalArgumentException(
                        String.format("Serie muy corta: %d observaciones (mínimo: %d)",
                                serie.size(), config.getPeriodosMinimos()));
            }

            // 2. Detectar y manejar valores nulos/infinitos
            List<Double> serieLimpia = limpiarSerie(serie);

            // 3. Determinar orden de diferenciación (d)
            int d = determinarOrdenDiferenciacion(serieLimpia, config.getMaxD());
            log.debug("✅ Orden de diferenciación: d={}", d);

            // 4. Aplicar diferenciación
            List<Double> serieDiferenciada = aplicarDiferenciacion(serieLimpia, d);

            // 5. Buscar mejor combinación de (p,q)
            ArimaModel mejorModelo = buscarMejorModelo(
                    serieLimpia, serieDiferenciada, d, config);

            if (mejorModelo == null || !mejorModelo.esValido()) {
                throw new RuntimeException("No se pudo ajustar un modelo ARIMA válido");
            }

            log.debug("✅ Mejor modelo: {}, AIC={}",
                    mejorModelo.getNotacion(),
                    String.format("%.2f", mejorModelo.getAic()));

            // 6. Generar pronóstico con intervalo
            ArimaForecast forecast = generarPronostico(
                    serieLimpia, mejorModelo, config);

            log.debug("✅ Pronóstico: {}, Intervalo: [{}, {}]",
                    String.format("%.2f", forecast.getPronostico()),
                    String.format("%.2f", forecast.getLimiteInferior()),
                    String.format("%.2f", forecast.getLimiteSuperior()));

            return forecast;

        } catch (Exception e) {
            log.error("❌ Error ajustando modelo ARIMA: {}", e.getMessage());
            throw new RuntimeException("Error en ajuste ARIMA: " + e.getMessage(), e);
        }
    }

    /**
     * Limpia la serie de valores nulos e infinitos
     */
    private List<Double> limpiarSerie(List<Double> serie) {
        return serie.stream()
                .filter(Objects::nonNull)
                .filter(Double::isFinite)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 DETERMINAR ORDEN DE DIFERENCIACIÓN
     *
     * Usa prueba de estacionariedad simplificada:
     * - Calcula ACF(1) de la serie
     * - Si |ACF(1)| > 0.9, la serie no es estacionaria → d++
     * - Máximo d según config
     */
    private int determinarOrdenDiferenciacion(List<Double> serie, int maxD) {

        List<Double> serieActual = new ArrayList<>(serie);
        int d = 0;

        while (d < maxD) {
            double acf1 = calcularACF(serieActual, 1);

            // Si ACF(1) < 0.9, consideramos estacionaria
            if (Math.abs(acf1) < 0.9) {
                break;
            }

            // Aplicar una diferenciación más
            serieActual = diferenciar(serieActual, 1);
            d++;
        }

        return d;
    }

    /**
     * Aplica diferenciación a la serie
     *
     * d=1: y'(t) = y(t) - y(t-1)
     * d=2: y''(t) = y'(t) - y'(t-1)
     */
    private List<Double> aplicarDiferenciacion(List<Double> serie, int orden) {
        List<Double> resultado = new ArrayList<>(serie);

        for (int i = 0; i < orden; i++) {
            resultado = diferenciar(resultado, 1);
        }

        return resultado;
    }

    /**
     * Diferencia una serie una vez
     */
    private List<Double> diferenciar(List<Double> serie, int lag) {
        List<Double> diferenciada = new ArrayList<>();

        for (int i = lag; i < serie.size(); i++) {
            diferenciada.add(serie.get(i) - serie.get(i - lag));
        }

        return diferenciada;
    }

    /**
     * 🔍 BUSCAR MEJOR MODELO ARIMA(p,d,q)
     *
     * Grid search sobre combinaciones de p y q
     * Selecciona el que minimiza AIC o BIC
     */
    private ArimaModel buscarMejorModelo(
            List<Double> serieOriginal,
            List<Double> serieDiferenciada,
            int d,
            ArimaConfig config) {

        ArimaModel mejorModelo = null;
        double mejorCriterio = Double.MAX_VALUE;

        // Probar todas las combinaciones de (p, q)
        for (int p = 0; p <= config.getMaxP(); p++) {
            for (int q = 0; q <= config.getMaxQ(); q++) {

                // Saltar (0,0,0) que no tiene sentido
                if (p == 0 && d == 0 && q == 0) continue;

                try {
                    ArimaModel modelo = ajustarModelo(
                            serieOriginal, serieDiferenciada, p, d, q);

                    if (modelo == null || !modelo.esValido()) continue;

                    // Seleccionar por criterio (AIC o BIC)
                    double criterio = "BIC".equals(config.getCriterioSeleccion()) ?
                            modelo.getBic() : modelo.getAic();

                    if (criterio < mejorCriterio) {
                        mejorCriterio = criterio;
                        mejorModelo = modelo;
                    }

                } catch (Exception e) {
                    log.debug("⚠️ No se pudo ajustar ARIMA({},{},{}): {}",
                            p, d, q, e.getMessage());
                }
            }
        }

        return mejorModelo;
    }

    /**
     * 🔧 AJUSTAR MODELO ARIMA(p,d,q)
     *
     * Implementación simplificada usando mínimos cuadrados
     */
    private ArimaModel ajustarModelo(
            List<Double> serieOriginal,
            List<Double> serieDiferenciada,
            int p, int d, int q) {

        int n = serieDiferenciada.size();

        // Necesitamos al menos max(p,q) + 1 observaciones
        if (n < Math.max(p, q) + 1) {
            return null;
        }

        // Para simplificar, usamos modelo AR(p) puro si q=0
        // o modelo MA(q) puro si p=0
        // (ARMA completo requiere optimización no lineal)

        if (q == 0) {
            // Modelo AR(p) puro - más simple de ajustar
            return ajustarAR(serieDiferenciada, p, d, n);
        } else if (p == 0) {
            // Modelo MA(q) puro
            return ajustarMA(serieDiferenciada, q, d, n);
        } else {
            // ARMA(p,q) - aproximación simple
            return ajustarARMA(serieDiferenciada, p, q, d, n);
        }
    }

    /**
     * Ajusta modelo AR(p) usando Yule-Walker
     */
    private ArimaModel ajustarAR(List<Double> serie, int p, int d, int n) {

        // Calcular autocovarianzas
        double[] acf = new double[p + 1];
        for (int k = 0; k <= p; k++) {
            acf[k] = calcularACF(serie, k);
        }

        // Resolver ecuaciones de Yule-Walker (simplificado)
        double[] coefs = new double[p];
        for (int i = 0; i < p; i++) {
            coefs[i] = acf[i + 1] / (acf[0] + 1e-10);
        }

        // Calcular residuos y métricas
        double[] residuos = calcularResiduosAR(serie, coefs);
        double sigmaSquared = varianza(residuos);

        // Calcular AIC y BIC
        double logLik = -0.5 * n * Math.log(2 * Math.PI * sigmaSquared) -
                0.5 * n;
        double numParams = p + 1; // coeficientes + sigma
        double aic = -2 * logLik + 2 * numParams;
        double bic = -2 * logLik + numParams * Math.log(n);

        return ArimaModel.builder()
                .p(p)
                .d(d)
                .q(0)
                .coeficientesAR(coefs)
                .coeficientesMA(new double[0])
                .intercepto(0)
                .aic(aic)
                .bic(bic)
                .errorEstandar(Math.sqrt(sigmaSquared))
                .varianzaResiduos(sigmaSquared)
                .numeroObservaciones(n)
                .fueDiferenciada(d > 0)
                .mediaOriginal(media(serie))
                .esEstacionario(true)
                .residuos(residuos)
                .build();
    }

    /**
     * Ajusta modelo MA(q) - aproximación simple
     */
    private ArimaModel ajustarMA(List<Double> serie, int q, int d, int n) {

        // MA requiere algoritmo iterativo (Box-Jenkins)
        // Usamos aproximación: ajustar AR(q) y convertir

        double[] coefs = new double[q];
        for (int i = 0; i < q; i++) {
            coefs[i] = calcularACF(serie, i + 1) * 0.8; // Aproximación
        }

        double[] residuos = calcularResiduosMA(serie, coefs);
        double sigmaSquared = varianza(residuos);

        double logLik = -0.5 * n * Math.log(2 * Math.PI * sigmaSquared) -
                0.5 * n;
        double numParams = q + 1;
        double aic = -2 * logLik + 2 * numParams;
        double bic = -2 * logLik + numParams * Math.log(n);

        return ArimaModel.builder()
                .p(0)
                .d(d)
                .q(q)
                .coeficientesAR(new double[0])
                .coeficientesMA(coefs)
                .intercepto(0)
                .aic(aic)
                .bic(bic)
                .errorEstandar(Math.sqrt(sigmaSquared))
                .varianzaResiduos(sigmaSquared)
                .numeroObservaciones(n)
                .fueDiferenciada(d > 0)
                .mediaOriginal(media(serie))
                .esEstacionario(true)
                .residuos(residuos)
                .build();
    }

    /**
     * Ajusta modelo ARMA(p,q) - aproximación básica
     */
    private ArimaModel ajustarARMA(List<Double> serie, int p, int q, int d, int n) {
        // Para ARMA completo, usamos el componente AR principalmente
        // y ajustamos residuos con MA
        return ajustarAR(serie, Math.max(p, q), d, n);
    }

    /**
     * 🔮 GENERAR PRONÓSTICO CON INTERVALO
     *
     * Según PDF Página 3: Intervalo = ŷ ± 1.96 × SE
     */
    private ArimaForecast generarPronostico(
            List<Double> serieOriginal,
            ArimaModel modelo,
            ArimaConfig config) {

        // Pronóstico depende del tipo de modelo
        double pronostico;

        if (modelo.getQ() == 0 && modelo.getP() > 0) {
            // Modelo AR(p)
            pronostico = pronosticarAR(serieOriginal, modelo);
        } else if (modelo.getP() == 0 && modelo.getQ() > 0) {
            // Modelo MA(q) - pronóstico es la media
            pronostico = media(serieOriginal);
        } else {
            // ARMA - usar componente AR
            pronostico = pronosticarAR(serieOriginal, modelo);
        }

        // Revertir diferenciación si fue aplicada
        if (modelo.getD() > 0) {
            // Para d=1: y(t+1) = y(t) + Δy(t+1)
            double ultimo = serieOriginal.get(serieOriginal.size() - 1);
            pronostico = ultimo + pronostico;
        }

        // Error estándar del pronóstico
        double se = modelo.getErrorEstandar();

        // Intervalo de predicción 95% (PDF Página 3)
        double z = config.getValorZ(); // 1.96 para 95%
        double li = pronostico - z * se;
        double ls = pronostico + z * se;

        return ArimaForecast.builder()
                .pronostico(pronostico)
                .limiteInferior(li)
                .limiteSuperior(ls)
                .errorEstandar(se)
                .nivelConfianza(config.getNivelConfianza())
                .build();
    }

    /**
     * Pronosticar un paso adelante con modelo AR
     */
    private double pronosticarAR(List<Double> serie, ArimaModel modelo) {
        int p = modelo.getP();
        double[] coefs = modelo.getCoeficientesAR();

        double forecast = 0;
        for (int i = 0; i < p && i < serie.size(); i++) {
            forecast += coefs[i] * serie.get(serie.size() - 1 - i);
        }

        return forecast;
    }

    // ========== FUNCIONES AUXILIARES ESTADÍSTICAS ==========

    /**
     * Calcula Función de Autocorrelación (ACF) para lag k
     */
    private double calcularACF(List<Double> serie, int k) {
        if (k >= serie.size()) return 0;

        double mean = media(serie);
        int n = serie.size();

        double cov0 = 0;
        for (double val : serie) {
            cov0 += Math.pow(val - mean, 2);
        }

        if (k == 0) return 1.0;

        double covk = 0;
        for (int i = k; i < n; i++) {
            covk += (serie.get(i) - mean) * (serie.get(i - k) - mean);
        }

        return covk / (cov0 + 1e-10);
    }

    /**
     * Calcula residuos de modelo AR
     */
    private double[] calcularResiduosAR(List<Double> serie, double[] coefs) {
        int p = coefs.length;
        int n = serie.size();
        double[] residuos = new double[n - p];

        for (int t = p; t < n; t++) {
            double pred = 0;
            for (int i = 0; i < p; i++) {
                pred += coefs[i] * serie.get(t - i - 1);
            }
            residuos[t - p] = serie.get(t) - pred;
        }

        return residuos;
    }

    /**
     * Calcula residuos de modelo MA (aproximación)
     */
    private double[] calcularResiduosMA(List<Double> serie, double[] coefs) {
        int n = serie.size();
        double[] residuos = new double[n];
        double mean = media(serie);

        // Aproximación: residuos como innovaciones
        for (int i = 0; i < n; i++) {
            residuos[i] = serie.get(i) - mean;
        }

        return residuos;
    }

    /**
     * Calcula media de una serie
     */
    private double media(List<Double> serie) {
        return serie.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calcula varianza de un array
     */
    private double varianza(double[] valores) {
        if (valores.length == 0) return 0;

        double mean = Arrays.stream(valores).average().orElse(0);
        double variance = 0;

        for (double val : valores) {
            variance += Math.pow(val - mean, 2);
        }

        return variance / valores.length;
    }
}