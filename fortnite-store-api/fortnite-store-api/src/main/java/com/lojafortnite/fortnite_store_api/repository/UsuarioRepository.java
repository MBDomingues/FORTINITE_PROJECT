package com.lojafortnite.fortnite_store_api.repository;

import com.lojafortnite.fortnite_store_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.itensAdquiridos ia LEFT JOIN FETCH ia.cosmetico WHERE u.id = :id")
    Optional<Usuario> findByIdWithItens(@Param("id") Long id);
}