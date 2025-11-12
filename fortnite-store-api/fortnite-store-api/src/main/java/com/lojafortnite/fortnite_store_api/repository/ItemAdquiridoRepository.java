package com.lojafortnite.fortnite_store_api.repository;

import com.lojafortnite.fortnite_store_api.entity.ItemAdquirido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItemAdquiridoRepository extends JpaRepository<ItemAdquirido, Long> {

    Optional<ItemAdquirido> findByUsuarioIdAndCosmeticoId(Long usuarioId, String cosmeticoId);

    boolean existsByUsuarioIdAndCosmeticoId(Long usuarioId, String cosmeticoId);
}