package com.resumen.nomina.application.service;

import com.resumen.nomina.application.repository.MenuRepository;
import com.resumen.nomina.domain.model.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    // Obtener todos los menús
    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    // Obtener menú por ID
    public Optional<Menu> getMenuById(Integer id) {
        return menuRepository.findByid(id);
    }

    // Obtener menú por ID de MongoDB
    public Optional<Menu> getMenuByMongoId(String mongoId) {
        return menuRepository.findById(mongoId);
    }

    // Obtener menús por negocio
    public List<Menu> getMenusByNegocio(String negocio) {
        return menuRepository.findByNegocio(negocio);
    }

    // Buscar menús por negocio (case insensitive)
    public List<Menu> searchMenusByNegocio(String negocio) {
        return menuRepository.findByNegocioIgnoreCase(negocio);
    }

    // Crear nuevo menú
    public Menu createMenu(Menu menu) {
        // Generar nuevo ID si no viene
        if (menu.getId() == null) {
            menu.setId(generateNextId());
        }

        // Verificar que no exista ya ese ID
        if (menuRepository.existsByid(menu.getId())) {
            throw new RuntimeException("Ya existe un menú con el ID: " + menu.getId());
        }

        return menuRepository.save(menu);
    }

    // Actualizar menú existente
    public Optional<Menu> updateMenu(Integer id, Menu menuDetails) {
        Optional<Menu> existingMenu = menuRepository.findByid(id);

        if (existingMenu.isPresent()) {
            Menu menu = existingMenu.get();
            menu.setNegocio(menuDetails.getNegocio());
            menu.setPuestos(menuDetails.getPuestos());
            return Optional.of(menuRepository.save(menu));
        }

        return Optional.empty();
    }

    // Actualizar menú por MongoDB ID
    public Optional<Menu> updateMenuByMongoId(String mongoId, Menu menuDetails) {
        Optional<Menu> existingMenu = menuRepository.findById(mongoId);

        if (existingMenu.isPresent()) {
            Menu menu = existingMenu.get();
            menu.setId(menuDetails.getId());
            menu.setNegocio(menuDetails.getNegocio());
            menu.setPuestos(menuDetails.getPuestos());
            return Optional.of(menuRepository.save(menu));
        }

        return Optional.empty();
    }

    // Eliminar menú por ID
    public boolean deleteMenu(Integer id) {
        if (menuRepository.existsByid(id)) {
            menuRepository.deleteByid(id);
            return true;
        }
        return false;
    }

    // Eliminar menú por MongoDB ID
    public boolean deleteMenuByMongoId(String mongoId) {
        if (menuRepository.existsById(mongoId)) {
            menuRepository.deleteById(mongoId);
            return true;
        }
        return false;
    }

    // Verificar si existe un menú
    public boolean existsMenu(Integer id) {
        return menuRepository.existsByid(id);
    }

    // Generar siguiente ID
    private Integer generateNextId() {
        List<Menu> lastMenu = menuRepository.findTopByOrderByIdDesc();
        if (lastMenu.isEmpty()) {
            return 1;
        }
        return lastMenu.get(0).getId() + 1;
    }
}