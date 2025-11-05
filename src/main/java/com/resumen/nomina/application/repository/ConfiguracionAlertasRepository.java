package com.resumen.nomina.application.repository;




import com.resumen.nomina.domain.model.ConfiguracionAlertas;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionAlertasRepository extends MongoRepository<ConfiguracionAlertas, String> {

    /**
     * Busca configuración por código
     */
    Optional<ConfiguracionAlertas> findByCodigoConfiguracion(String codigo);

    /**
     * Busca configuración activa por código
     */
    Optional<ConfiguracionAlertas> findByCodigoConfiguracionAndActivaTrue(String codigo);

    /**
     * Verifica si existe configuración activa
     */
    boolean existsByCodigoConfiguracionAndActivaTrue(String codigo);
}