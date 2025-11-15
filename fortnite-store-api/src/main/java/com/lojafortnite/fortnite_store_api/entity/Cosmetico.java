package com.lojafortnite.fortnite_store_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


//Classe entidade Cosmeticos para o BD
@Entity
@Table(name = "TB_COSMETICO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cosmetico {
    @Id
    private String id;

    private String nome;
    private String tipo;
    private String raridade;
    private String descricao;
    private String urlImagem;


    @Column(nullable = false)
    private Integer preco = 0;

    @Column(nullable = false)
    private Boolean isNew = false;

    @Column(nullable = false)
    private Boolean isForSale = false;

    private Instant dataInclusao;

    private Boolean isBundle = false;

    @Lob
    private String bundleItemsJson;

    @Column(name = "cores_json", length = 1000)
    private String coresJson;
}
