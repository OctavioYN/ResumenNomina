package com.resumen.nomina.domain.model;

import lombok.Data;
import org.bson.types.Decimal128;

@Data
public class Indicador {
    private int puesto;
    private String sucursal;
    private String FcDetalle5;
    private String FcDetalle6;
    private int negocio;
    private int conceptoDetalle;
    private String periodoAnterior;
    private double valorAnterior;
    private String periodoActual;
    private double valorActual;
    private double diferencia;
    private double variacion;

}
