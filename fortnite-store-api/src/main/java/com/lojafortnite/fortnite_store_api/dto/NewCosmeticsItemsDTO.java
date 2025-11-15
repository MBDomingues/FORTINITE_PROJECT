package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewCosmeticsItemsDTO {

    private List<CosmeticoApiDTO> br = new ArrayList<>();
    private List<CosmeticoApiDTO> cars = new ArrayList<>();
    private List<CosmeticoApiDTO> tracks = new ArrayList<>();
    private List<CosmeticoApiDTO> instruments = new ArrayList<>();
    private List<CosmeticoApiDTO> lego = new ArrayList<>();
    private List<CosmeticoApiDTO> beans = new ArrayList<>();
    private List<CosmeticoApiDTO> legoKits = new ArrayList<>();

    // Metodo para juntar tudo em uma lista só e facilitar o processamento
    public List<CosmeticoApiDTO> getTodosOsItens() {
        List<CosmeticoApiDTO> todos = new ArrayList<>();
        if(br != null) todos.addAll(br);
        if(cars != null) todos.addAll(cars);
        if(tracks != null) todos.addAll(tracks);
        if(instruments != null) todos.addAll(instruments);
        if(lego != null) todos.addAll(lego);
        if(beans != null) todos.addAll(beans);
        if(legoKits != null) todos.addAll(legoKits);
        return todos;
    }
}