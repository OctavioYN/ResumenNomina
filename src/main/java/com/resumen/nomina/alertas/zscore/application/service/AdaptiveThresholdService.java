package com.resumen.nomina.alertas.zscore.application.service;

import com.resumen.nomina.alertas.zscore.domain.model.ZScoreConfig;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 📈 SERVICIO DE UMBRALES ADAPTATIVOS SEGÚN PDF
 * Implementa la estrategia basada en volatilidad histórica
 */
@Slf4j
@Service
public class AdaptiveThresholdService {

    /**
     * Calcula márgenes dinámicos según la estrategia del PDF:
     * - σ < 1%: margen fijo de 1.5%
     * - 1% ≤ σ < 5%: 1.3×σ
     * - σ ≥ 5%: usa σ directamente
     */
    public double calcularMargenAdaptativo(double desviacionEstandar, ZScoreConfig config) {
        // Validar entrada
        if (desviacionEstandar < 0) {
            log.warn("Desviación estándar negativa: {}, usando valor absoluto", desviacionEstandar);
            desviacionEstandar = Math.abs(desviacionEstandar);
        }

        double margen;

        if (desviacionEstandar < config.getUmbralEstable()) {
            // Series muy estables: margen fijo de 1.5%
            margen = config.getFactorSeriesEstables() * config.getUmbralEstable();
            log.debug("📈 Serie ESTABLE (σ < 1%): Margen = {} × 1% = {:.4f}",
                    config.getFactorSeriesEstables(), margen);
        } else if (desviacionEstandar < config.getUmbralVolatilidad()) {
            // Volatilidad media: 1.3×σ
            margen = config.getFactorVolatilidadMedia() * desviacionEstandar;
            log.debug("📈 Serie MEDIA (1% ≤ σ < 5%): Margen = {} × {:.4f} = {:.4f}",
                    config.getFactorVolatilidadMedia(), desviacionEstandar, margen);
        } else {
            // Alta volatilidad: usa σ directamente
            margen = desviacionEstandar;
            log.debug("📈 Serie VOLÁTIL (σ ≥ 5%): Margen = σ = {:.4f}", desviacionEstandar);
        }

        // Aplicar límites de seguridad
        return aplicarLimitesSeguridad(margen, config);
    }

    /**
     * Aplica límites de seguridad según PDF:
     * El margen final está acotado entre 1% y 20%
     */
    private double aplicarLimitesSeguridad(double margen, ZScoreConfig config) {
        double margenAjustado = Math.max(margen, config.getLimiteMinimoMargen());
        margenAjustado = Math.min(margenAjustado, config.getLimiteMaximoMargen());

        if (margen != margenAjustado) {
            log.debug("🔧 Ajustando margen: {:.4f} → {:.4f} (Límites: {:.0f}% - {:.0f}%)",
                    margen, margenAjustado,
                    config.getLimiteMinimoMargen() * 100,
                    config.getLimiteMaximoMargen() * 100);
        }

        return margenAjustado;
    }

    /**
     * 🔴 VALIDACIÓN TRIPLE SEGÚN PDF
     * 1. Superar umbrales dinámicos
     * 2. Diferencia absoluta > 1%
     * 3. Z-score > 1
     */
    public boolean validarCondicionesAlerta(double variacionActual, double zScore,
                                            double variacionMedia, double desviacionEstandar,
                                            ZScoreConfig config) {

        // 1. Calcular límites adaptativos
        double margen = calcularMargenAdaptativo(desviacionEstandar, config);
        double limiteInferior = variacionMedia - margen;
        double limiteSuperior = variacionMedia + margen;

        // 2. Aplicar validación triple
        boolean cond1 = superaUmbralesDinamicos(variacionActual, limiteInferior, limiteSuperior);
        boolean cond2 = diferenciaSignificativa(variacionActual, config);
        boolean cond3 = zScoreSignificativo(zScore, config);

        boolean alertaActiva = cond1 && cond2 && cond3;

        log.debug("🔍 Validación Triple - Variación: {:.2f}%, Límites: [{:.2f}% a {:.2f}%], " +
                        "Z: {:.2f}, Condiciones: [{}|{}|{}] → Alerta: {}",
                variacionActual * 100, limiteInferior * 100, limiteSuperior * 100,
                zScore, cond1, cond2, cond3, alertaActiva);

        return alertaActiva;
    }

    private boolean superaUmbralesDinamicos(double variacion, double limiteInferior, double limiteSuperior) {
        return variacion < limiteInferior || variacion > limiteSuperior;
    }

    private boolean diferenciaSignificativa(double variacion, ZScoreConfig config) {
        return Math.abs(variacion) > config.getUmbralDiferenciaMinima();
    }

    private boolean zScoreSignificativo(double zScore, ZScoreConfig config) {
        return Math.abs(zScore) > config.getUmbralZScoreMinimo();
    }

    /**
     * Calcula límites completos para un indicador
     */
    public LimitesAdaptativos calcularLimitesCompletos(double variacionMedia, double desviacionEstandar,
                                                       ZScoreConfig config) {
        double margen = calcularMargenAdaptativo(desviacionEstandar, config);
        double limiteInferior = variacionMedia - margen;
        double limiteSuperior = variacionMedia + margen;

        return LimitesAdaptativos.builder()
                .variacionMedia(variacionMedia)
                .desviacionEstandar(desviacionEstandar)
                .margenAdaptativo(margen)
                .limiteInferior(limiteInferior)
                .limiteSuperior(limiteSuperior)
                .build();
    }

    /**
     * DTO para resultados de límites adaptativos
     */
    @Data
    @Builder
    public static class LimitesAdaptativos {
        private double variacionMedia;
        private double desviacionEstandar;
        private double margenAdaptativo;
        private double limiteInferior;
        private double limiteSuperior;
    }
}