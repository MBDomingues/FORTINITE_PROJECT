package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagesDTO {
    // Usado no Battle Royale
    private String smallIcon;
    private String icon;
    private String featured;

    // Usado em Carros, Lego, Instrumentos
    private String small;
    private String large;
}