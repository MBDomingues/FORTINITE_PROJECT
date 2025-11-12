package com.lojafortnite.fortnite_store_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;


//Classe entidade historico de transacoes para o BD
@Entity
@Table(name = "TB_HISTORICO_TRANSACAO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoTransacao {

    public enum TipoTransacao {
        COMPRA, DEVOLUCAO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historico_seq_generator")
    @SequenceGenerator(
            name = "historico_seq_generator",
            sequenceName = "HISTORICO_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cosmetico_id")
    private Cosmetico cosmetico;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @Column(nullable = false)
    private Integer valor;

    @Column(nullable = false)
    private LocalDateTime dataTransacao = LocalDateTime.now();
}