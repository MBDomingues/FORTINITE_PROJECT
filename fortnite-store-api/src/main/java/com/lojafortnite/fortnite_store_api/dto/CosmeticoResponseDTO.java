package com.lojafortnite.fortnite_store_api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
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

    // --- NOVOS CAMPOS ---
    private Boolean isBundle;
    private List<String> bundleItems; // Lista de IDs que vêm no pacote
    private List<String> cores;       // Cores para o degradê (ex: Marvel, DC)

    // Construtor atualizado
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

        // Mapeia o novo campo de Bundle
        this.isBundle = cosmetico.getIsBundle();

        // Converte as Strings JSON do banco para Listas Java
        this.bundleItems = converterStringParaLista(cosmetico.getBundleItemsJson());
        this.cores = converterStringParaLista(cosmetico.getCoresJson());
    }

    // Método auxiliar para transformar "[id1, id2]" em uma List<String> real
    private List<String> converterStringParaLista(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            // Remoção simples de caracteres JSON para evitar uso complexo de ObjectMapper aqui dentro
            String limpo = json.replace("[", "").replace("]", "").replace("\"", "");
            String[] itens = limpo.split(",");
            List<String> lista = new ArrayList<>();
            for (String item : itens) {
                if (!item.trim().isEmpty()) {
                    lista.add(item.trim());
                }
            }
            return lista;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}