package com.lojafortnite.fortnite_store_api.dto;

import lombok.Data;

// DTO para receber os dados da requisição de cadastro
@Data
public class UsuarioCadastroRequest {
    private String email;
    private String senha;
    private String nome;
}