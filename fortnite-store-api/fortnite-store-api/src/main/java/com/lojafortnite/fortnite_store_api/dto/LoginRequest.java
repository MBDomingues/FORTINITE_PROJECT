package com.lojafortnite.fortnite_store_api.dto;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String senha;
}