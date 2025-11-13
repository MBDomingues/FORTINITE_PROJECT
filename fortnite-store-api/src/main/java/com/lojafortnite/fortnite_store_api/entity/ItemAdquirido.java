package com.lojafortnite.fortnite_store_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.NaturalId;

import java.time.LocalDateTime;


//Classe entidade item adquirido para o BD
@Entity
@Table(name = "TB_ITEM_ADQUIRIDO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemAdquirido {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_adquirido_seq_generator")
    @SequenceGenerator(
            name = "item_adquirido_seq_generator",
            sequenceName = "ITEM_ADQUIRIDO_SEQ",
            allocationSize = 1
    )
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cosmetico_id", nullable = false)
    private Cosmetico cosmetico;

    @Column(nullable = false)
    private LocalDateTime dataCompra = LocalDateTime.now();

    @NaturalId
    private String uniqueUserCosmeticoKey;
}