package com.resumen.nomina.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoCarga {
    private boolean success;
    private String mensaje;
    private int totalRegistros;
    private int registrosExitosos;
    private int registrosConError;
    private List<String> errores;
    private LocalDateTime fechaCarga;
    private String usuarioCarga;

    // Estadísticas adicionales
    private Long tiempoProcesamientoMs;
    private String archivoOriginal;
}
