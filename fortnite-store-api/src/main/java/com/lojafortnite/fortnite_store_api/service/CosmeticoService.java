package com.lojafortnite.fortnite_store_api.service;

import com.lojafortnite.fortnite_store_api.dto.CosmeticoFiltroRequest;
import com.lojafortnite.fortnite_store_api.dto.CosmeticoResponseDTO;
import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import com.lojafortnite.fortnite_store_api.repository.CosmeticoRepository;
import com.lojafortnite.fortnite_store_api.repository.ItemAdquiridoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CosmeticoService {

    @Autowired private CosmeticoRepository cosmeticoRepository;
    @Autowired
    private ItemAdquiridoRepository itemAdquiridoRepository;


    @Transactional(readOnly = true)
    public Page<CosmeticoResponseDTO> listarCosmeticos(
            CosmeticoFiltroRequest filtro,
            Pageable pageable,
            Long userId
    ) {
        var spec = CosmeticoSpecification.comFiltros(filtro);
        Page<Cosmetico> cosmeticosPage = cosmeticoRepository.findAll(spec, pageable);

        return cosmeticosPage.map(cosmetico -> {
            CosmeticoResponseDTO dto = new CosmeticoResponseDTO(cosmetico);

            // Se o usuário estiver logado, checar se ele possui o item
            if (userId != null) {
                boolean isAdquirido = itemAdquiridoRepository.existsByUsuarioIdAndCosmeticoId(userId, cosmetico.getId());
                dto.setIsAdquirido(isAdquirido);
            }
            return dto;
        });
    }
}