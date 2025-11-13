package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CosmeticoApiDTO {

    private String id;
    private String name;
    private String description;
    private Instant added;

    // Mapeia os objetos aninhados
    private TypeDTO type;
    private RarityDTO rarity;
    private ImagesDTO images;
}