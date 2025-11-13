package com.lojafortnite.fortnite_store_api.service;

import com.lojafortnite.fortnite_store_api.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class TokenService {

    // 1. Chave Secreta para assinar o token.
    // EM UM PROJETO REAL, ISSO DEVE VIR DE UM ARQUIVO DE PROPRIEDADES (application.properties)
    // E DEVE SER UMA STRING LONGA E SEGURA.
    private final SecretKey CHAVE_SECRETA = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private final long EXPIRACAO_MS = 86400000; // 24 * 60 * 60 * 1000

    /**
     * Gera um novo Token JWT para o usuário autenticado.
     */
    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + EXPIRACAO_MS);

        return Jwts.builder()
                .setIssuer("API Fortnite Store") // Quem emitiu o token
                .setSubject(usuario.getEmail())   // O "dono" do token (nosso username é o e-mail)
                .setIssuedAt(agora)               // Data de emissão
                .setExpiration(dataExpiracao)     // Data de expiração
                .signWith(CHAVE_SECRETA)          // Assinatura com a chave secreta
                .compact();
    }

    /**
     * Valida o token e retorna o "Subject" (o e-mail do usuário)
     */
    public String getSubject(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(CHAVE_SECRETA)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            // Se o token for inválido, expirado, etc.
            return null;
        }
    }
}