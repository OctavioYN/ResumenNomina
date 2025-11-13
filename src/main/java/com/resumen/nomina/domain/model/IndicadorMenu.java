package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IndicadorMenu {
    // Getters y Setters
    private Integer idIndicador;
    private String Indicador;

    // Constructores
    public IndicadorMenu() {}

    public IndicadorMenu(Integer idIndicador, String indicador) {
        this.idIndicador = idIndicador;
        this.Indicador = indicador;
    }

}