package com.lojafortnite.fortnite_store_api.dto;

import com.lojafortnite.fortnite_store_api.entity.Usuario;
import lombok.Data;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UsuarioPerfilResponseDTO {
    private Long id;
    private String email;
    private Integer creditos;
    private String nome;
    private List<CosmeticoResponseDTO> itensAdquiridos;

    public UsuarioPerfilResponseDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.creditos = usuario.getCreditos();
        this.nome = usuario.getNome();

        // Mapeia a lista de entidades para uma lista de DTOs
        if (usuario.getItensAdquiridos() != null) {
            this.itensAdquiridos = usuario.getItensAdquiridos().stream()
                    // Pega o 'cosmetico' de dentro do 'itemAdquirido'
                    .map(itemAdquirido -> new CosmeticoResponseDTO(itemAdquirido.getCosmetico()))
                    .collect(Collectors.toList());
        } else {
            this.itensAdquiridos = Collections.emptyList();
        }
    }
}