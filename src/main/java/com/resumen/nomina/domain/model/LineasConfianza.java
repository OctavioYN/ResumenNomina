package com.resumen.nomina.domain.model;

import lombok.Getter;
import lombok.Setter;

// DTO para líneas de confianza
@Setter
@Getter
public class LineasConfianza {
    // Getters y Setters
    private Long media;
    private Long superior1DS;
    private Long inferior1DS;
    private Long superior15DS;
    private Long inferior15DS;

    // Constructores
    public LineasConfianza() {}

    public LineasConfianza(Long media, Long superior1DS, Long inferior1DS,
                           Long superior15DS, Long inferior15DS) {
        this.media = media;
        this.superior1DS = superior1DS;
        this.inferior1DS = inferior1DS;
        this.superior15DS = superior15DS;
        this.inferior15DS = inferior15DS;
    }

    @Override
    public String toString() {
        return "LineasConfianza{" +
                "media=" + media +
                ", superior1DS=" + superior1DS +
                ", inferior1DS=" + inferior1DS +
                ", superior15DS=" + superior15DS +
                ", inferior15DS=" + inferior15DS +
                '}';
    }
}