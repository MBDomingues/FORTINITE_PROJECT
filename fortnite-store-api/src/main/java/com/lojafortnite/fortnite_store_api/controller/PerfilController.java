package com.lojafortnite.fortnite_store_api.controller;

import com.lojafortnite.fortnite_store_api.dto.HistoricoTransacaoDTO;
import com.lojafortnite.fortnite_store_api.dto.UsuarioPerfilResponseDTO;
import com.lojafortnite.fortnite_store_api.entity.Usuario;
import com.lojafortnite.fortnite_store_api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/v1/perfis")
public class PerfilController {

    @Autowired private UsuarioService usuarioService;

    
    @GetMapping
    public ResponseEntity<Page<UsuarioPerfilResponseDTO>> listarTodosOsPerfis(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Page<UsuarioPerfilResponseDTO> perfis = usuarioService.listarTodosPerfis(pageable);
        return ResponseEntity.ok(perfis);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioPerfilResponseDTO> obterDetalhesDoPerfil(@PathVariable Long id) {

        UsuarioPerfilResponseDTO perfilDTO = usuarioService.obterPerfilComItens(id);
        return ResponseEntity.ok(perfilDTO);
    }

    
    @GetMapping("/me")
    public ResponseEntity<UsuarioPerfilResponseDTO> obterMeuPerfil(@AuthenticationPrincipal(expression = "id") Long userId) {

        UsuarioPerfilResponseDTO perfilDTO = usuarioService.obterPerfilComHistorico(userId);
        return ResponseEntity.ok(perfilDTO);
    }

    
    @GetMapping("/me/historico")
    public ResponseEntity<Page<HistoricoTransacaoDTO>> obterMeuHistorico(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PageableDefault(size = 10, sort = "dataTransacao", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        
        Page<HistoricoTransacaoDTO> historico = usuarioService.listarHistoricoDoUsuario(userId, pageable);

        return ResponseEntity.ok(historico);
    }
}