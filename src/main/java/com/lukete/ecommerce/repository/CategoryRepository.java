package com.lukete.ecommerce.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lukete.ecommerce.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByName(String name);

    Optional<Category> findByNameIgnoreCase(String name);
}