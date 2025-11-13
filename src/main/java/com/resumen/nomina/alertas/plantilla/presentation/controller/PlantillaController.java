package com.resumen.nomina.alertas.plantilla.presentation.controller;


import com.resumen.nomina.alertas.plantilla.application.service.PlantillaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/plantilla")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlantillaController {

    private final PlantillaService plantillaService;

    @GetMapping("/activos")
    public ResponseEntity<List<Map<String, Object>>> obtenerActivos(
            @RequestParam String periodoActual) {

        log.info("📊 Obteniendo datos de activos para período: {}", periodoActual);

        try {
            List<Map<String, Object>> resultado = plantillaService.obtenerDatosActivos(periodoActual);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error obteniendo activos: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}