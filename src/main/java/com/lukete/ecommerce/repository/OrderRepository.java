package com.lukete.ecommerce.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lukete.ecommerce.domain.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

}
