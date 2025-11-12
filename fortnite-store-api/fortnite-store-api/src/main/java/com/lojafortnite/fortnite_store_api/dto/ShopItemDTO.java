package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopItemDTO {
    private Integer finalPrice;

    private List<CosmeticoApiDTO> brItems;
}