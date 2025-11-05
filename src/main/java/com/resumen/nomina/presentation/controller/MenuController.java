package com.resumen.nomina.presentation.controller;

import com.resumen.nomina.application.service.MenuService;
import com.resumen.nomina.domain.model.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/menus")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired
    private MenuService menuService;

    // GET - Obtener todos los menús
    @GetMapping
    public ResponseEntity<List<Menu>> getAllMenus() {
        List<Menu> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus);
    }

    // GET - Obtener menú por ID
    @GetMapping("/{id}")
    public ResponseEntity<Menu> getMenuById(@PathVariable Integer id) {
        Optional<Menu> menu = menuService.getMenuById(id);
        return menu.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET - Obtener menú por MongoDB ID
    @GetMapping("/mongo/{mongoId}")
    public ResponseEntity<Menu> getMenuByMongoId(@PathVariable String mongoId) {
        Optional<Menu> menu = menuService.getMenuByMongoId(mongoId);
        return menu.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET - Buscar menús por negocio
    @GetMapping("/negocio/{negocio}")
    public ResponseEntity<List<Menu>> getMenusByNegocio(@PathVariable String negocio) {
        List<Menu> menus = menuService.getMenusByNegocio(negocio);
        return ResponseEntity.ok(menus);
    }

    // GET - Buscar menús por negocio (case insensitive)
    @GetMapping("/search")
    public ResponseEntity<List<Menu>> searchMenusByNegocio(@RequestParam String negocio) {
        List<Menu> menus = menuService.searchMenusByNegocio(negocio);
        return ResponseEntity.ok(menus);
    }

    // POST - Crear nuevo menú
    @PostMapping
    public ResponseEntity<Menu> createMenu(@RequestBody Menu menu) {
        try {
            Menu savedMenu = menuService.createMenu(menu);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMenu);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT - Actualizar menú por ID
    @PutMapping("/{id}")
    public ResponseEntity<Menu> updateMenu(@PathVariable Integer id, @RequestBody Menu menuDetails) {
        Optional<Menu> updatedMenu = menuService.updateMenu(id, menuDetails);
        return updatedMenu.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT - Actualizar menú por MongoDB ID
    @PutMapping("/mongo/{mongoId}")
    public ResponseEntity<Menu> updateMenuByMongoId(@PathVariable String mongoId, @RequestBody Menu menuDetails) {
        Optional<Menu> updatedMenu = menuService.updateMenuByMongoId(mongoId, menuDetails);
        return updatedMenu.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE - Eliminar menú por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Integer id) {
        boolean deleted = menuService.deleteMenu(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // DELETE - Eliminar menú por MongoDB ID
    @DeleteMapping("/mongo/{mongoId}")
    public ResponseEntity<Void> deleteMenuByMongoId(@PathVariable String mongoId) {
        boolean deleted = menuService.deleteMenuByMongoId(mongoId);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // HEAD - Verificar si existe un menú
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkMenuExists(@PathVariable Integer id) {
        boolean exists = menuService.existsMenu(id);
        return exists ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}