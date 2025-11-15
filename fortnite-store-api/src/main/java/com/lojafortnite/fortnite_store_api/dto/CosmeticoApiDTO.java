package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonAlias; // Importe isto
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CosmeticoApiDTO {

    private String id;

    @JsonAlias({"name", "title"})
    private String name;

    private String description;
    private Instant added;

    private TypeDTO type;
    private RarityDTO rarity;
    private ImagesDTO images;
    private SeriesDTO series;

    private String artist;
    private String albumArt;

    public String getImagemPrincipal() {
        if (images != null) {
            if (images.getIcon() != null) return images.getIcon();
            if (images.getSmallIcon() != null) return images.getSmallIcon();
            if (images.getFeatured() != null) return images.getFeatured();

            if (images.getLarge() != null) return images.getLarge();
            if (images.getSmall() != null) return images.getSmall();
        }

        if (albumArt != null) return albumArt;

        return null;
    }

    public String getRaridadeTexto() {
        if (rarity != null && rarity.getDisplayValue() != null) {
            return rarity.getDisplayValue();
        }
        return "Comum";
    }

    public String getTipoTexto() {
        if (type != null && type.getDisplayValue() != null) {
            return type.getDisplayValue();
        }
        if (artist != null) return "Música";
        return "Item";
    }
}