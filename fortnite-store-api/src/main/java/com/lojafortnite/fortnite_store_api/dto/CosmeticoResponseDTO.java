package com.lojafortnite.fortnite_store_api.dto;

import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor // Construtor padrão necessário
public class CosmeticoResponseDTO {

    private String id;
    private String nome;
    private String tipo;
    private String raridade;
    private String descricao;
    private String urlImagem;
    private Integer preco;
    private Boolean isNew;
    private Boolean isForSale;
    private Instant dataInclusao;
    private Boolean isAdquirido = false;

    // Construtor para mapeamento da Entidade (usando getters explícitos)
    public CosmeticoResponseDTO(Cosmetico cosmetico) {
        this.id = cosmetico.getId();
        this.nome = cosmetico.getNome();
        this.tipo = cosmetico.getTipo();
        this.raridade = cosmetico.getRaridade();
        this.descricao = cosmetico.getDescricao();
        this.urlImagem = cosmetico.getUrlImagem();
        this.preco = cosmetico.getPreco();
        this.isNew = cosmetico.getIsNew();
        this.isForSale = cosmetico.getIsForSale();
        this.dataInclusao = cosmetico.getDataInclusao();
    }
}