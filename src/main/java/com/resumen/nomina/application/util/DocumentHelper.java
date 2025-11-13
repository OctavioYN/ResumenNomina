package com.resumen.nomina.application.util;

import org.bson.Document;

/**
 * Utilidad para extraer valores numéricos de forma segura desde Document de MongoDB
 */
public class DocumentHelper {

    /**
     * Obtiene un Double de forma segura desde un Document
     * Maneja Integer, Long, Double y convierte automáticamente
     */
    public static Double getDoubleValue(Document doc, String field) {
        if (doc == null || field == null) {
            return 0.0;
        }

        Object value = doc.get(field);

        if (value == null) {
            return 0.0;
        }

        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }

        // Intentar parsear como String
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Obtiene un Integer de forma segura
     */
    public static Integer getIntegerValue(Document doc, String field) {
        if (doc == null || field == null) {
            return 0;
        }

        Object value = doc.get(field);

        if (value == null) {
            return 0;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Obtiene un Boolean de forma segura
     */
    public static Boolean getBooleanValue(Document doc, String field) {
        if (doc == null || field == null) {
            return false;
        }

        Object value = doc.get(field);

        if (value == null) {
            return false;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Obtiene un String de forma segura con trim
     */
    public static String getStringValue(Document doc, String field) {
        if (doc == null || field == null) {
            return "";
        }

        Object value = doc.get(field);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }
}

