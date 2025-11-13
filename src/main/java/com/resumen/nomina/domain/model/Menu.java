package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Setter
@Getter
@Document(collection = "catalogoTextos")
public class Menu {
    // Getters y Setters
    @Id
    private String _id;
    private Integer id;
    private String Negocio;
    private List<Puesto> Puestos;

    // Constructores
    public Menu() {}

    public Menu(Integer id, String negocio, List<Puesto> puestos) {
        this.id = id;
        this.Negocio = negocio;
        this.Puestos = puestos;
    }

}