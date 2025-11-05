package com.resumen.nomina.application.repository;

import com.resumen.nomina.domain.model.Menu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends MongoRepository<Menu, String> {

    // Buscar por id numérico
    Optional<Menu> findByid(Integer id);

    // Buscar por negocio
    List<Menu> findByNegocio(String negocio);

    // Buscar por negocio ignorando mayúsculas/minúsculas
    @Query("{'Negocio': {$regex: ?0, $options: 'i'}}")
    List<Menu> findByNegocioIgnoreCase(String negocio);

    // Verificar si existe por id numérico
    boolean existsByid(Integer id);

    // Eliminar por id numérico
    void deleteByid(Integer id);

    // Obtener el último id para generar el siguiente
    @Query(value = "{}", sort = "{'id': -1}")
    List<Menu> findTopByOrderByIdDesc();
}