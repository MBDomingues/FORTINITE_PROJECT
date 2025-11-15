package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonAlias; // Importe isto
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CosmeticoApiDTO {

    private String id;

    // Lê "name" (BR/Carros) OU "title" (Músicas)
    @JsonAlias({"name", "title"})
    private String name;

    private String description;
    private Instant added;

    private TypeDTO type;
    private RarityDTO rarity;
    private ImagesDTO images;
    private SeriesDTO series;

    // Campos específicos para Músicas (Tracks)
    private String artist;
    private String albumArt;

    // Metodo auxiliar para pegar a imagem correta independente do tipo
    public String getImagemPrincipal() {
        if (images != null) {
            if (images.getIcon() != null) return images.getIcon();
            if (images.getSmallIcon() != null) return images.getSmallIcon();
        }
        // Se for música, a imagem vem em 'albumArt'
        if (albumArt != null) return albumArt;

        return null; // Sem imagem
    }

    // Metodo auxiliar para garantir que sempre tenha uma raridade
    public String getRaridadeTexto() {
        if (rarity != null && rarity.getDisplayValue() != null) {
            return rarity.getDisplayValue();
        }
        return "Comum"; // Valor padrão se a API não mandar raridade (ex: algumas músicas)
    }

    // Metodo auxiliar para garantir que sempre tenha um tipo
    public String getTipoTexto() {
        if (type != null && type.getDisplayValue() != null) {
            return type.getDisplayValue();
        }
        // Se não tem tipo, tentamos adivinhar ou retornamos genérico
        if (artist != null) return "Música";
        return "Item";
    }
}