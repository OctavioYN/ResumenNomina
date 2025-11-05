package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Puesto {
    // Getters y Setters
    private Integer idPuesto;
    private Integer idFuncion;
    private String Puesto;
    private List<IndicadorMenu> Indicadores;

    // Constructores
    public Puesto() {}

    public Puesto(Integer idPuesto,Integer idFuncion, String puesto, List<IndicadorMenu> indicadores) {
        this.idPuesto = idPuesto;
        this.idFuncion=idFuncion;
        this.Puesto = puesto;
        this.Indicadores = indicadores;
    }

}