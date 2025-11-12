package com.lojafortnite.fortnite_store_api.controller;

import com.lojafortnite.fortnite_store_api.dto.CosmeticoFiltroRequest;
import com.lojafortnite.fortnite_store_api.dto.CosmeticoResponseDTO;
import com.lojafortnite.fortnite_store_api.entity.Usuario;
import com.lojafortnite.fortnite_store_api.service.CosmeticoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para gerenciar os itens cosméticos (Skins, Gestos, etc.).
 * Endpoint Base: /api/v1/cosmeticos
 */
@RestController
@RequestMapping("/api/v1/cosmeticos")
public class CosmeticoController {

    @Autowired
    private CosmeticoService cosmeticoService;

    /**
     * Rota: GET /api/v1/cosmeticos
     * Objetivo: Listar itens de forma geral com paginação e filtros dinâmicos.
     * Usado na aba "Todos os Itens".
     */
    @GetMapping
    public ResponseEntity<Page<CosmeticoResponseDTO>> listarGeral(
            CosmeticoFiltroRequest filtros,
            // @PageableDefault define o padrão caso o JS não envie nada
            @PageableDefault(page = 0, size = 40, sort = "dataInclusao", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = resolverUsuarioId(principal);

        Page<CosmeticoResponseDTO> pagina = cosmeticoService.listarCosmeticos(filtros, pageable, userId);

        return ResponseEntity.ok(pagina);
    }

    /**
     * Rota: GET /api/v1/cosmeticos/loja
     * Objetivo: Listar apenas itens disponíveis para venda.
     * Usado na vitrine principal.
     */
    @GetMapping("/loja")
    public ResponseEntity<List<CosmeticoResponseDTO>> listarLoja(
            CosmeticoFiltroRequest filtros,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = resolverUsuarioId(principal);

        filtros.setIsForSale(true);

        Page<CosmeticoResponseDTO> pagina = cosmeticoService.listarCosmeticos(filtros, Pageable.unpaged(), userId);

        return ResponseEntity.ok(pagina.getContent());
    }

    /**
     * Rota: GET /api/v1/cosmeticos/novos
     * Objetivo: Listar apenas itens marcados como "Novos".
     */
    @GetMapping("/novos")
    public ResponseEntity<List<CosmeticoResponseDTO>> listarNovos(
            CosmeticoFiltroRequest filtros,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = resolverUsuarioId(principal);

        filtros.setIsNew(true);

        Page<CosmeticoResponseDTO> pagina = cosmeticoService.listarCosmeticos(filtros, Pageable.unpaged(), userId);

        return ResponseEntity.ok(pagina.getContent());
    }

    /**
     * metodo auxiliar privado.
     * Extrai o ID do usuário do objeto de segurança do Spring.
     * Retorna null se o usuário não estiver logado (visitante).
     */
    private Long resolverUsuarioId(Object principal) {
        if (principal instanceof Usuario) {
            return ((Usuario) principal).getId();
        }
        return null;
    }
}