package com.resumen.nomina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🔧 CONFIGURACIÓN DE ALERTAS
 * Almacena parámetros configurables para el sistema de alertas
 * Colección: ConfiguracionAlertas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ConfiguracionAlertas")
public class ConfiguracionAlertas {

    @Id
    private String _id;

    // ========== IDENTIFICACIÓN ==========

    /**
     * Identificador único de la configuración
     * Usar "DEFAULT" para la configuración principal
     */
    private String codigoConfiguracion;

    private String descripcion;
    private Boolean activa;

    // ========== PERÍODOS HISTÓRICOS ==========

    /**
     * Mínimo de períodos históricos requeridos para calcular alertas
     * Valor por defecto: 12
     */
    private Integer periodosMinimosHistoricos;

    /**
     * Períodos necesarios para considerar un modelo como robusto
     * Valor por defecto: 24
     */
    private Integer periodosModeloRobusto;

    // ========== UMBRALES Z-SCORE ==========

    /**
     * Umbral para severidad CRÍTICA (Z-Score absoluto ≥ valor)
     * Valor por defecto: 2.5
     */
    private Double umbralCritico;

    /**
     * Umbral para severidad ALTA (Z-Score absoluto ≥ valor)
     * Valor por defecto: 1.96
     */
    private Double umbralAlto;

    /**
     * Umbral para severidad MODERADA (Z-Score absoluto ≥ valor)
     * Valor por defecto: 1.0
     */
    private Double umbralModerado;

    // ========== INTERVALO DE CONFIANZA ARIMA ==========

    /**
     * Nivel de confianza para intervalo ARIMA (95% = 1.96)
     * Valor por defecto: 1.96
     */
    private Double nivelConfianzaArima;

    // ========== EXCLUSIONES ==========

    /**
     * Lista de conceptoDetalle a excluir del cálculo
     * Por defecto: [1011] (empleados)
     */
    private List<Integer> conceptosExcluidos;

    /**
     * Lista de negocios a excluir (opcional)
     */
    private List<Integer> negociosExcluidos;

    /**
     * Lista de puestos a excluir (opcional)
     */
    private List<Integer> puestosExcluidos;

    // ========== METADATA ==========

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private String usuarioCreacion;
    private String usuarioModificacion;
    private String version;

    // ========== MÉTODOS DE UTILIDAD ==========

    /**
     * Crea configuración por defecto si no existe
     */
    public static ConfiguracionAlertas crearConfiguracionPorDefecto() {
        return ConfiguracionAlertas.builder()
                .codigoConfiguracion("DEFAULT")
                .descripcion("Configuración por defecto del sistema de alertas")
                .activa(true)
                .periodosMinimosHistoricos(12)
                .periodosModeloRobusto(24)
                .umbralCritico(2.5)
                .umbralAlto(1.96)
                .umbralModerado(1.0)
                .nivelConfianzaArima(1.96)
                .conceptosExcluidos(List.of(1011))
                .negociosExcluidos(List.of())
                .puestosExcluidos(List.of())
                .fechaCreacion(LocalDateTime.now())
                .usuarioCreacion("SISTEMA")
                .version("1.0")
                .build();
    }

    /**
     * Valida que todos los parámetros estén dentro de rangos aceptables
     */
    public void validar() {
        if (periodosMinimosHistoricos == null || periodosMinimosHistoricos < 1) {
            throw new IllegalArgumentException("periodosMinimosHistoricos debe ser >= 1");
        }

        if (periodosModeloRobusto == null || periodosModeloRobusto < periodosMinimosHistoricos) {
            throw new IllegalArgumentException("periodosModeloRobusto debe ser >= periodosMinimosHistoricos");
        }

        if (umbralCritico == null || umbralCritico <= umbralAlto) {
            throw new IllegalArgumentException("umbralCritico debe ser > umbralAlto");
        }

        if (umbralAlto == null || umbralAlto <= umbralModerado) {
            throw new IllegalArgumentException("umbralAlto debe ser > umbralModerado");
        }

        if (umbralModerado == null || umbralModerado < 0) {
            throw new IllegalArgumentException("umbralModerado debe ser >= 0");
        }

        if (nivelConfianzaArima == null || nivelConfianzaArima < 0) {
            throw new IllegalArgumentException("nivelConfianzaArima debe ser >= 0");
        }
    }
}