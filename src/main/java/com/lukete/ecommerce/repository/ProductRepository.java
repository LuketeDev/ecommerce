package com.lukete.ecommerce.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lukete.ecommerce.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

}
