package com.resumen.nomina.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperacionRequest {
    private String operacion;  // "LIMPIAR", "RECALCULAR", "BACKUP"
    private String usuario;
    private Object parametros;
}
