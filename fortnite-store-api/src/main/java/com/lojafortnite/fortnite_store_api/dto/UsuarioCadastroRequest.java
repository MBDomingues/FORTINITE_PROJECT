package com.lojafortnite.fortnite_store_api.dto;

import lombok.Data;


@Data
public class UsuarioCadastroRequest {
    private String email;
    private String senha;
    private String nome;
}