
package com.resumen.nomina.presentation.controller;

import com.resumen.nomina.application.service.SemanaService;
import com.resumen.nomina.domain.model.SemanaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/semanas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SemanaController {

    private final SemanaService semanaService;

    /**
     * Obtiene la semana correspondiente a la fecha proporcionada y las 4 semanas anteriores
     *
     * @param fecha Fecha en formato yyyyMMdd (ejemplo: 20250930)
     * @return Lista de 5 semanas (actual + 4 anteriores)
     */
    @GetMapping("/{fecha}")
    public ResponseEntity<List<SemanaResponse>> obtenerSemanasAnteriores(
            @PathVariable String fecha) {

        log.info("Solicitud recibida para obtener semanas de la fecha: {}", fecha);

        try {
            List<SemanaResponse> semanas = semanaService.obtenerSemanasAnteriores(fecha);
            return ResponseEntity.ok(semanas);

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Error inesperado al procesar la solicitud", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint alternativo con query parameter
     */
    @GetMapping
    public ResponseEntity<List<SemanaResponse>> obtenerSemanasConQuery(
            @RequestParam(name = "fecha") String fecha) {

        return obtenerSemanasAnteriores(fecha);
    }
}