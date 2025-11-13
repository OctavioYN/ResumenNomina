


package com.resumen.nomina.application.repository;

import com.resumen.nomina.domain.model.IndicadorPromedio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicadorPromedioRepository extends MongoRepository<IndicadorPromedio, String> {

    // Buscar por combinación específica
    Optional<IndicadorPromedio> findByNegocioAndPuestoAndConceptoDetalleAndSemana(
            Integer negocio, Integer puesto, Integer conceptoDetalle, String semana);

    // Listar por negocio, puesto e indicador ordenado por fecha
    @Query(value = "{'negocio': ?0, 'puesto': ?1, 'conceptoDetalle': ?2}",
            sort = "{'anio': 1, 'numeroSemana': 1}")
    List<IndicadorPromedio> findByNegocioPuestoIndicador(Integer negocio, Integer puesto, Integer conceptoDetalle);

    // Verificar existencia
    boolean existsByNegocioAndPuestoAndConceptoDetalleAndSemana(
            Integer negocio, Integer puesto, Integer conceptoDetalle, String semana);

    // Eliminar por combinación
    void deleteByNegocioAndPuestoAndConceptoDetalleAndSemana(
            Integer negocio, Integer puesto, Integer conceptoDetalle, String semana);
}