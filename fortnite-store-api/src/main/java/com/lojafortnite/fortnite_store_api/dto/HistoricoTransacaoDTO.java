package com.lojafortnite.fortnite_store_api.dto;

import com.lojafortnite.fortnite_store_api.entity.HistoricoTransacao;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoricoTransacaoDTO {
    private Long id;
    private HistoricoTransacao.TipoTransacao tipo;
    private Integer valor;
    private LocalDateTime dataTransacao;

    private String cosmeticoId;
    private String cosmeticoNome;
    private String cosmeticoUrlImagem;

    public HistoricoTransacaoDTO(HistoricoTransacao historico) {
        this.id = historico.getId();
        this.tipo = historico.getTipo();
        this.valor = historico.getValor();
        this.dataTransacao = historico.getDataTransacao();

        if (historico.getCosmetico() != null) {
            this.cosmeticoId = historico.getCosmetico().getId();
            this.cosmeticoNome = historico.getCosmetico().getNome();
            this.cosmeticoUrlImagem = historico.getCosmetico().getUrlImagem();
        }
    }
}