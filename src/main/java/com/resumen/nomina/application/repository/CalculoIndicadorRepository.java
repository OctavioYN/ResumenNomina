package com.resumen.nomina.application.repository;

import com.resumen.nomina.domain.model.Indicador;
import java.util.List;

public interface CalculoIndicadorRepository {
    List<Indicador> obtenerIndicadoresPorPeriodos(List<String> periodos);
}
