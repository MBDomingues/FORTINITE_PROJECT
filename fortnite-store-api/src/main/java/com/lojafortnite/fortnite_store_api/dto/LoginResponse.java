package com.lojafortnite.fortnite_store_api.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Facilita a criação
public class LoginResponse {
    private String token;
}