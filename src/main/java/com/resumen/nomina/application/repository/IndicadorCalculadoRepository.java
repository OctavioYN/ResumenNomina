package com.resumen.nomina.application.repository;

import com.resumen.nomina.domain.model.IndicadorCalculado;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicadorCalculadoRepository extends MongoRepository<IndicadorCalculado, String> {

    // Buscar por puesto y negocio
    List<IndicadorCalculado> findByPuestoAndNegocio(Integer puesto, Integer negocio);

    // Buscar por periodo actual
    List<IndicadorCalculado> findByPeriodoActual(String periodoActual);

    // Buscar por rango de fechas de cálculo
    List<IndicadorCalculado> findByFechaCalculoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Buscar los cálculos más recientes por negocio
    @Query(value = "{'negocio': ?0}", sort = "{'fechaCalculo': -1}")
    List<IndicadorCalculado> findLatestByNegocio(Integer negocio);

    // Buscar por tipo de cálculo
    List<IndicadorCalculado> findByTipoCalculo(String tipoCalculo);

    // Obtener el último cálculo para un puesto específico
    @Query(value = "{'puesto': ?0, 'negocio': ?1}", sort = "{'fechaCalculo': -1}")
    Optional<IndicadorCalculado> findLatestByPuestoAndNegocio(Integer puesto, Integer negocio);

    // Eliminar cálculos anteriores a una fecha
    void deleteByFechaCalculoBefore(LocalDateTime fecha);

    // Contar cálculos por periodo
    long countByPeriodoActual(String periodoActual);

    // Buscar por sucursal
    List<IndicadorCalculado> findBySucursal(String sucursal);

    // Buscar combinación específica
    List<IndicadorCalculado> findByPuestoAndNegocioAndConceptoDetalle(Integer puesto, Integer negocio, Integer conceptoDetalle);
}