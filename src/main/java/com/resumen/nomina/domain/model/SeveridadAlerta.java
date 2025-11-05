/*package com.resumen.nomina.domain.model;


import lombok.Getter;

@Getter
public enum SeveridadAlerta {
    NORMAL("NORMAL", 0, "#4CAF50"),
    MODERADA("MODERADA", 1, "#FFC107"),
    ALTA("ALTA", 2, "#FF9800"),
    CRITICA("CRÍTICA", 3, "#F44336");

    // Getters
    private final String descripcion;
    private final int nivel;
    private final String colorHex;

    SeveridadAlerta(String descripcion, int nivel, String colorHex) {
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.colorHex = colorHex;
    }

    public static SeveridadAlerta fromZScore(double zScore) {
        double absZScore = Math.abs(zScore);
        if (absZScore >= 2.5) return CRITICA;
        if (absZScore >= 1.96) return ALTA;
        if (absZScore >= 1.0) return MODERADA;
        return NORMAL;
    }

    public boolean esMayorOIgualQue(SeveridadAlerta otra) {
        return this.nivel >= otra.nivel;
    }
}
*/

// ============================================================
// 1. REFACTORIZAR EL ENUM: SeveridadAlerta.java
// ============================================================

package com.resumen.nomina.domain.model;

import lombok.Getter;

@Getter
public enum SeveridadAlerta {
    NORMAL("NORMAL", 0, "#4CAF50"),
    MODERADA("MODERADA", 1, "#FFC107"),
    ALTA("ALTA", 2, "#FF9800"),
    CRITICA("CRÍTICA", 3, "#F44336");

    private final String descripcion;
    private final int nivel;
    private final String colorHex;

    SeveridadAlerta(String descripcion, int nivel, String colorHex) {
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.colorHex = colorHex;
    }

    /**
     * ✅ NUEVO: Calcula severidad desde Z-Score usando configuración BD
     * @param zScore Valor Z-Score calculado
     * @param umbralCritico Z-Score para CRÍTICA (ej: 2.5)
     * @param umbralAlto Z-Score para ALTA (ej: 1.96)
     * @param umbralModerado Z-Score para MODERADA (ej: 1.0)
     * @return SeveridadAlerta correspondiente
     */
    public static SeveridadAlerta fromZScore(double zScore,
                                             Double umbralCritico,
                                             Double umbralAlto,
                                             Double umbralModerado) {
        double absZScore = Math.abs(zScore);

        // Usar valores por defecto si vienen nulos desde BD
        double critico = umbralCritico != null ? umbralCritico : 2.5;
        double alto = umbralAlto != null ? umbralAlto : 1.96;
        double moderado = umbralModerado != null ? umbralModerado : 1.0;

        if (absZScore >= critico) return CRITICA;
        if (absZScore >= alto) return ALTA;
        if (absZScore >= moderado) return MODERADA;
        return NORMAL;
    }

    /**
     * ✅ SOBRECARGA: Versión simplificada (hacia atrás compatible)
     */
    public static SeveridadAlerta fromZScore(double zScore) {
        return fromZScore(zScore, null, null, null);
    }

    /**
     * Comparación de severidad
     */
    public boolean esMayorQue(SeveridadAlerta otra) {
        return this.nivel > otra.nivel;
    }

    public boolean esMayorOIgualQue(SeveridadAlerta otra) {
        return this.nivel >= otra.nivel;
    }

    public String getEmoji() {
        return switch (this) {
            case CRITICA -> "🔴";
            case ALTA -> "🟠";
            case MODERADA -> "🟡";
            case NORMAL -> "🟢";
        };
    }
}
