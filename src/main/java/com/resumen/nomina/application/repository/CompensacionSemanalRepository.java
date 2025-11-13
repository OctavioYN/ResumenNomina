package com.resumen.nomina.application.repository;

import com.resumen.nomina.domain.model.CompensacionSemanal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompensacionSemanalRepository extends MongoRepository<CompensacionSemanal, String> {

    // Buscar por semana específica
    Optional<CompensacionSemanal> findBySemana(String semana);

    // Buscar por año
    List<CompensacionSemanal> findByAnioOrderByNumeroSemanaAsc(Integer anio);

    // Buscar desde un año específico (para gráficas históricas)
    @Query("{'anio': {$gte: ?0}}")
    List<CompensacionSemanal> findFromYear(Integer anio);

    // Obtener las últimas 2 semanas para comparación
    @Query(value = "{}", sort = "{'anio': -1, 'numeroSemana': -1}")
    List<CompensacionSemanal> findTop2ByOrderByAnioDescNumeroSemanaDesc();

    // Obtener la semana más reciente
    @Query(value = "{}", sort = "{'anio': -1, 'numeroSemana': -1}")
    Optional<CompensacionSemanal> findTopByOrderByAnioDescNumeroSemanaDesc();

    // Obtener rango de semanas para estadísticas
    @Query(value = "{'anio': {$gte: ?0, $lte: ?1}}", sort = "{'anio': 1, 'numeroSemana': 1}")
    List<CompensacionSemanal> findByAnioRange(Integer anioInicio, Integer anioFin);

    // Buscar por rango de fechas de cálculo
    List<CompensacionSemanal> findByFechaCalculoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Verificar si existe una semana
    boolean existsBySemana(String semana);

    // Eliminar por semana
    void deleteBySemana(String semana);

    // Contar registros por año
    long countByAnio(Integer anio);

    // Obtener semanas de un año específico ordenadas
    List<CompensacionSemanal> findByAnioOrderByNumeroSemana(Integer anio);

    // Actualizar flag de última semana
    @Query("{'esUltimaSemana': true}")
    List<CompensacionSemanal> findByEsUltimaSemanaTrue();

    // Buscar por tipo de cálculo
    List<CompensacionSemanal> findByTipoCalculo(String tipoCalculo);

    // Obtener estadísticas desde una fecha
    @Query(value = "{'fechaCalculo': {$gte: ?0}}", sort = "{'anio': 1, 'numeroSemana': 1}")
    List<CompensacionSemanal> findFromDate(LocalDateTime fecha);


    // Agregar estos métodos al Repository de CompensacionSemanal

    /**
     * Busca una compensación por semana específica (formato YYYYWW)
     */
    //Optional<CompensacionSemanal> findBySemana(String semana);

    /**
     * Obtiene las 2 últimas semanas a partir de una semana específica (inclusive)
     * @param semana formato YYYYWW (ejemplo: "202539")
     */
    @Query(value = "{'semana': {$lte: ?0}}", sort = "{'anio': -1, 'numeroSemana': -1}")
    List<CompensacionSemanal> findTop2BySemanaLessThanEqualOrderByAnioDescNumeroSemanaDesc(String semana, org.springframework.data.domain.Pageable pageable);


}