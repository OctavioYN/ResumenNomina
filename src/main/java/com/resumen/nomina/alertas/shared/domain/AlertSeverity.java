package com.resumen.nomina.alertas.shared.domain;


import lombok.Getter;

/**
 * 🚨 NIVELES DE SEVERIDAD PARA ALERTAS
 */
@Getter
public enum AlertSeverity {
    NORMAL("NORMAL", 0, "#4CAF50", "🟢"),
    MODERADA("MODERADA", 1, "#FFC107", "🟡"),
    ALTA("ALTA", 2, "#FF9800", "🟠"),
    CRITICA("CRÍTICA", 3, "#F44336", "🔴");

    private final String descripcion;
    private final int nivel;
    private final String colorHex;
    private final String emoji;

    AlertSeverity(String descripcion, int nivel, String colorHex, String emoji) {
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.colorHex = colorHex;
        this.emoji = emoji;
    }

    /**
     * Calcula severidad desde Z-Score usando configuración
     */
    public static AlertSeverity fromZScore(double zScore, double umbralCritico,
                                           double umbralAlto, double umbralModerado) {
        double absZScore = Math.abs(zScore);

        if (absZScore >= umbralCritico) return CRITICA;
        if (absZScore >= umbralAlto) return ALTA;
        if (absZScore >= umbralModerado) return MODERADA;
        return NORMAL;
    }

    /**
     * Versión simplificada con valores por defecto
     */
    public static AlertSeverity fromZScore(double zScore) {
        return fromZScore(zScore, 2.5, 1.96, 1.0);
    }

    public boolean esMayorQue(AlertSeverity otra) {
        return this.nivel > otra.nivel;
    }

    public boolean esMayorOIgualQue(AlertSeverity otra) {
        return this.nivel >= otra.nivel;
    }
}