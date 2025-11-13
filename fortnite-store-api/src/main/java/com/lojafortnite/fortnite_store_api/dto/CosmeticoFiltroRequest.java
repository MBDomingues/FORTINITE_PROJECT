package com.lojafortnite.fortnite_store_api.dto;

import lombok.Data;

import java.time.LocalDate;

// DTO para receber os parâmetros de busca e filtro do frontend
@Data
public class CosmeticoFiltroRequest {

    private String nome;
    private String tipo;
    private String raridade;

    // Filtros de status (booleanos)
    private Boolean isNew;
    private Boolean isForSale;
    private Boolean isPromo;

    // Filtros de data (intervalo de datas)
    private LocalDate dataInclusaoInicio;
    private LocalDate dataInclusaoFim;
}