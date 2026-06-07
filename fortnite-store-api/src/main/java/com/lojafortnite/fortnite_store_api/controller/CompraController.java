package com.lojafortnite.fortnite_store_api.controller;

import com.lojafortnite.fortnite_store_api.entity.Usuario;
import com.lojafortnite.fortnite_store_api.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CompraController {

    @Autowired
    private CompraService compraService;

    
    @PostMapping("/compra/{cosmeticoId}")
    public ResponseEntity<String> comprar(
            @PathVariable String cosmeticoId,
            @AuthenticationPrincipal Usuario usuario) {

        
        if (usuario == null) {
            return ResponseEntity.status(401).body("Não autorizado.");
        }

        try {
            compraService.comprarCosmetico(usuario.getId(), cosmeticoId);
            return ResponseEntity.ok("Compra realizada com sucesso! V-bucks restantes: " + usuario.getCreditos());

        } catch (RuntimeException e) {
            
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    
    @PostMapping("/devolucao/{cosmeticoId}")
    public ResponseEntity<String> devolver(
            @PathVariable String cosmeticoId,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(401).body("Não autorizado.");
        }

        try {
            compraService.devolverCosmetico(usuario.getId(), cosmeticoId);
            return ResponseEntity.ok("Devolução processada com sucesso. V-bucks estornados.");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}