package com.lojafortnite.fortnite_store_api.repository;

import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface CosmeticoRepository extends JpaRepository<Cosmetico, String>, JpaSpecificationExecutor<Cosmetico> {

    // Método para resetar o status 'À Venda' antes da sincronização da loja
    @Transactional
    @Modifying
    @Query("UPDATE Cosmetico c SET c.isForSale = false")
    void resetAllIsForSaleStatus();

    // Método para resetar o status 'Novo'
    @Transactional
    @Modifying
    @Query("UPDATE Cosmetico c SET c.isNew = false")
    void resetAllIsNewStatus();
}