package com.resumen.nomina.alertas.shared.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ⚙️ CONFIGURACIÓN DE PROPIEDADES PARA ALERTAS
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.alertas")
public class AlertasProperties {

    private Zscore zscore = new Zscore();
    private Database database = new Database();

    @Data
    public static class Zscore {
        private boolean enabled = true;
        private String version = "2.0";
        private boolean validacionTriple = true;
        private boolean estrategiaAdaptativa = true;
        private int periodosMinimos = 12;
    }

    @Data
    public static class Database {
        private String collectionIndicadores = "IndicadoresCalculados";
        private String collectionConfiguracion = "ConfiguracionAlertas";
        private int timeoutSeconds = 30;
    }
}