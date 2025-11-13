package com.resumen.nomina.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

// ========== DTO PARA DATOS INTELIGENCIA ==========

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosInteligenciaRow {
    // Campos PKI (Primary Key Identifiers)
    private Integer pkiPuesto;           // 2647
    private Integer pkiSucursal;         // 0
    private Integer pkiEmpleado;         // 0
    private Integer pkiCDGenerico;       // 1592
    private Integer pkiPais;             // 1
    private String pkiPeriodo;           // "202542"
    private Integer pkiGrupoNegocio;     // 5
    private Integer pkiCanal;            // 0
    private Integer pkiConceptoDetalle;  // 1011

    // Campos FN (Fields Numeric)
    private Double fnValor;              // 10.0
    private Double fnDetalle1;           // 0.0
    private Double fnDetalle2;           // 0.0

    // Campos PK (Primary Key)
    private Integer pkcDetalle3;         // 0

    // Campos FC (Fields Character/String)
    private String fcDetalle4;           // "Presta Prenda"
    private String fcDetalle5;           // "Líder de Tienda"
    private String fcDetalle6;           // "Empleado"

    private Double fnDetalle7;           // 0.0
}

// ========== DTO PARA CATÁLOGO TEXTOS ==========

// ========== RESULTADO DE CARGA ==========

// ========== REQUEST PARA OPERACIONES ADMIN ==========

