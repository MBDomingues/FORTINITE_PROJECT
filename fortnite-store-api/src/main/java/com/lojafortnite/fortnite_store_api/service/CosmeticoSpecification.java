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

            if (filtro.getNome() != null && !filtro.getNome().isEmpty()) {
                predicates.add(builder.like(builder.lower(root.get("nome")), "%" + filtro.getNome().toLowerCase() + "%"));
            }

            if (filtro.getTipo() != null && !filtro.getTipo().isEmpty()) {
                predicates.add(builder.equal(root.get("tipo"), filtro.getTipo()));
            }

            if (filtro.getRaridade() != null && !filtro.getRaridade().isEmpty()) {
                predicates.add(builder.equal(root.get("raridade"), filtro.getRaridade()));
            }

            if (Boolean.TRUE.equals(filtro.getIsNew())) {
                predicates.add(builder.isTrue(root.get("isNew")));
            }

            if (Boolean.TRUE.equals(filtro.getIsForSale())) {
                predicates.add(builder.isTrue(root.get("isForSale")));
            }

            if (Boolean.TRUE.equals(filtro.getIsPromo())) {
                predicates.add(builder.isTrue(root.get("isPromo")));
            }


            if (filtro.getDataInclusaoInicio() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dataInclusao"), filtro.getDataInclusaoInicio().atStartOfDay()));
            }

            if (filtro.getDataInclusaoFim() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dataInclusao"), filtro.getDataInclusaoFim().atTime(23, 59, 59)));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}