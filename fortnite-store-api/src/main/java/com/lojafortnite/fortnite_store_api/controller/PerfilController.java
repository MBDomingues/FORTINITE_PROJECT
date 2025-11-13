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


/**
 * Controlador REST para visualizar perfis de usuário e históricos.
 * Endpoint Base: /api/v1/perfis
 */
@RestController
@RequestMapping("/api/v1/perfis")
public class PerfilController {

    @Autowired private UsuarioService usuarioService;

    /**
     * Rota: GET /api/v1/perfis
     * Objetivo: Listar todos os usuários cadastrados no sistema.
     * Usado na aba "Usuários" do front-end.
     * Retorna uma lista paginada.
     */
    @GetMapping
    public ResponseEntity<Page<UsuarioPerfilResponseDTO>> listarTodosOsPerfis(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Page<UsuarioPerfilResponseDTO> perfis = usuarioService.listarTodosPerfis(pageable);
        return ResponseEntity.ok(perfis);
    }

    /**
     * Rota: GET /api/v1/perfis/{id}
     * Objetivo: Ver o perfil público de um usuário específico.
     * Retorna dados como nome, email e os itens que ele possui.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioPerfilResponseDTO> obterDetalhesDoPerfil(@PathVariable Long id) {

        UsuarioPerfilResponseDTO perfilDTO = usuarioService.obterPerfilComItens(id);
        return ResponseEntity.ok(perfilDTO);
    }

    /**
     * Rota: GET /api/v1/perfis/me
     * Objetivo: Obter os dados do PRÓPRIO usuário que está logado.
     * O @AuthenticationPrincipal extrai o ID do token JWT automaticamente.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioPerfilResponseDTO> obterMeuPerfil(@AuthenticationPrincipal(expression = "id") Long userId) {

        UsuarioPerfilResponseDTO perfilDTO = usuarioService.obterPerfilComHistorico(userId);
        return ResponseEntity.ok(perfilDTO);
    }

    /**
     * Rota: GET /api/v1/perfis/me/historico
     * Objetivo: Listar o histórico de transações (Compras e Devoluções) do usuário logado.
     * Retorna uma lista paginada ordenada por data (mais recente primeiro).
     */
    @GetMapping("/me/historico")
    public ResponseEntity<Page<HistoricoTransacaoDTO>> obterMeuHistorico(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PageableDefault(size = 10, sort = "dataTransacao", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        // Chama o serviço para buscar apenas o histórico deste usuário específico
        Page<HistoricoTransacaoDTO> historico = usuarioService.listarHistoricoDoUsuario(userId, pageable);

        return ResponseEntity.ok(historico);
    }
}