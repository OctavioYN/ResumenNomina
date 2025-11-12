package com.resumen.nomina.alertas.zscore.application.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 🔧 UTILIDADES PARA MANEJO CONSISTENTE DE FORMATOS NUMÉRICOS
 */
@Slf4j
@Component
public class ZScoreNumberHelper {

    /**
     * Normaliza valores a formato decimal (0.XX) independientemente del formato de entrada
     */
    public static double normalizarADecimal(Double valor) {
        if (valor == null) {
            return 0.0;
        }

        // Si el valor absoluto es mayor que 1, asumimos que es porcentaje
        if (Math.abs(valor) > 1.0) {
            double decimal = valor / 100.0;
            log.trace("🔄 Convertiendo porcentaje a decimal: {}% → {}", valor, decimal);
            return decimal;
        }

        return valor;
    }

    /**
     * Convierte decimal a porcentaje para visualización
     */
    public static double aPorcentaje(double decimal) {
        return decimal * 100.0;
    }

    /**
     * Convierte porcentaje a decimal para cálculos
     */
    public static double aDecimal(double porcentaje) {
        return porcentaje / 100.0;
    }

    /**
     * Redondea un valor al número de decimales especificado
     */
    public static double redondear(double valor, int decimales) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return 0.0;
        }

        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }

    /**
     * Calcula Z-Score de forma segura
     */
    public static double calcularZScore(double valorActual, double media, double desviacionEstandar) {
        if (desviacionEstandar == 0) {
            log.warn("⚠️ Desviación estándar cero, no se puede calcular Z-Score");
            return 0.0;
        }

        double zScore = (valorActual - media) / desviacionEstandar;
        log.trace("🎯 Z-Score calculado: ({:.4f} - {:.4f}) / {:.4f} = {:.4f}",
                valorActual, media, desviacionEstandar, zScore);

        return zScore;
    }

    /**
     * Valida que un valor esté en rango razonable para variaciones porcentuales
     */
    public static boolean esVariacionValida(Double valor) {
        if (valor == null) return false;

        // Las variaciones deberían estar típicamente entre -100% y +1000%
        // pero permitimos un rango más amplio para casos extremos
        return valor >= -10.0 && valor <= 50.0; // -1000% a +5000% en decimal
    }
}