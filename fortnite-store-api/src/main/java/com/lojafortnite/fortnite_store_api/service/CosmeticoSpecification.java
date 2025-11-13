package com.lojafortnite.fortnite_store_api.service;

import com.lojafortnite.fortnite_store_api.dto.CosmeticoFiltroRequest;
import com.lojafortnite.fortnite_store_api.entity.Cosmetico;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CosmeticoSpecification {

    /**
     * Constrói a Specification dinâmica baseada nos filtros do DTO.
     */
    public static Specification<Cosmetico> comFiltros(CosmeticoFiltroRequest filtro) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro por NOME (Texto Livre) [cite: 44]
            if (filtro.getNome() != null && !filtro.getNome().isEmpty()) {
                predicates.add(builder.like(builder.lower(root.get("nome")), "%" + filtro.getNome().toLowerCase() + "%"));
            }

            // 2. Filtro por TIPO [cite: 45]
            if (filtro.getTipo() != null && !filtro.getTipo().isEmpty()) {
                predicates.add(builder.equal(root.get("tipo"), filtro.getTipo()));
            }

            // 3. Filtro por RARIDADE [cite: 46]
            if (filtro.getRaridade() != null && !filtro.getRaridade().isEmpty()) {
                predicates.add(builder.equal(root.get("raridade"), filtro.getRaridade()));
            }

            // 4. Filtro por STATUS: NOVOS [cite: 48]
            if (Boolean.TRUE.equals(filtro.getIsNew())) {
                predicates.add(builder.isTrue(root.get("isNew")));
            }

            // 5. Filtro por STATUS: À VENDA [cite: 49]
            if (Boolean.TRUE.equals(filtro.getIsForSale())) {
                predicates.add(builder.isTrue(root.get("isForSale")));
            }

            if (Boolean.TRUE.equals(filtro.getIsPromo())) {
                predicates.add(builder.isTrue(root.get("isPromo")));
            }


            // 7. Filtro por DATA DE INCLUSÃO (Intervalo) [cite: 47]
            if (filtro.getDataInclusaoInicio() != null) {
                // Filtra datas no banco que são MAIORES OU IGUAIS ao início
                predicates.add(builder.greaterThanOrEqualTo(root.get("dataInclusao"), filtro.getDataInclusaoInicio().atStartOfDay()));
            }

            if (filtro.getDataInclusaoFim() != null) {
                // Filtra datas no banco que são MENORES OU IGUAIS ao fim do dia
                predicates.add(builder.lessThanOrEqualTo(root.get("dataInclusao"), filtro.getDataInclusaoFim().atTime(23, 59, 59)));
            }

            // Combina todas as condições com AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}