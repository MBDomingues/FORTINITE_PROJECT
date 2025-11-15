package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FortniteApiResponseDTO {

    private int status;

    private List<CosmeticoApiDTO> data;
}