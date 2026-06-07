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


@RestController
@RequestMapping("/api/v1/cosmeticos")
public class CosmeticoController {

    @Autowired
    private CosmeticoService cosmeticoService;

    
    @GetMapping
    public ResponseEntity<Page<CosmeticoResponseDTO>> listarGeral(
            CosmeticoFiltroRequest filtros,
            
            @PageableDefault(page = 0, size = 40, sort = "dataInclusao", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = resolverUsuarioId(principal);

        Page<CosmeticoResponseDTO> pagina = cosmeticoService.listarCosmeticos(filtros, pageable, userId);

        return ResponseEntity.ok(pagina);
    }

    
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

    
    private Long resolverUsuarioId(Object principal) {
        if (principal instanceof Usuario) {
            return ((Usuario) principal).getId();
        }
        return null;
    }
}