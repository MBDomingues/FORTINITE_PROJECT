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

    private final SecretKey CHAVE_SECRETA = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private final long EXPIRACAO_MS = 86400000; 


    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + EXPIRACAO_MS);

        return Jwts.builder()
                .setIssuer("API Fortnite Store")
                .setSubject(usuario.getEmail())
                .setIssuedAt(agora)
                .setExpiration(dataExpiracao)
                .signWith(CHAVE_SECRETA)
                .compact();
    }


    public String getSubject(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(CHAVE_SECRETA)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}