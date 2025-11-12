package com.lojafortnite.fortnite_store_api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuração de Utilitários de Segurança.
 * Isola a criação de beans básicos para evitar ciclos de dependência na inicialização.
 */
@Configuration
public class UtilityConfig {
    /**
     * Define o algoritmo de criptografia de senhas da aplicação.
     * O Spring Security usará este Bean automaticamente para:
     * 1. Criptografar a senha ao criar um usuário (Cadastro).
     * 2. Verificar se a senha bate com o hash no banco (Login).
     * * Usamos BCrypt: um algoritmo de hash forte e padrão de mercado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}