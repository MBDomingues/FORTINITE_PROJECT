package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagesDTO {
    private String smallIcon;
    private String icon;
    private String featured;
}