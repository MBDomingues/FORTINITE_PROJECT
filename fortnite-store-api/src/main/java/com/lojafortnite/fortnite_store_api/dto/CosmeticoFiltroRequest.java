package com.lojafortnite.fortnite_store_api.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CosmeticoFiltroRequest {

    private String nome;
    private String tipo;
    private String raridade;

    private Boolean isNew;
    private Boolean isForSale;
    private Boolean isPromo;

    private LocalDate dataInclusaoInicio;
    private LocalDate dataInclusaoFim;
}