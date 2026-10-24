package com.lukete.ecommerce.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProductSpecification implements Specification<Product> {
    private UUID categoryId;
    private Boolean active;

    @Override
    public @Nullable Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(this.categoryId)) {
            predicates.add(criteriaBuilder.equal(root.get("category").get("id"), this.categoryId));
        }
        if (Objects.nonNull(active)) {
            predicates.add(criteriaBuilder.equal(root.get("active"), this.active));
        } else {
            predicates.add(criteriaBuilder.equal(root.get("active"), true));
        }
        return criteriaBuilder.and(predicates.stream().toArray(Predicate[]::new));
    }

}
