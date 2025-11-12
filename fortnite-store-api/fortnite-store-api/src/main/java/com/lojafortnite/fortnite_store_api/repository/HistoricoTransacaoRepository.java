package com.lojafortnite.fortnite_store_api.repository;

import com.lojafortnite.fortnite_store_api.entity.HistoricoTransacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoTransacaoRepository extends JpaRepository<HistoricoTransacao, Long> {

    Page<HistoricoTransacao> findByUsuarioId(Long usuarioId, Pageable pageable);
}