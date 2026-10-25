package com.lukete.ecommerce.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.lukete.ecommerce.exception.InsufficientProductStockException;
import com.lukete.ecommerce.exception.InvalidStockQuantityException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 150, nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void setDefaults() {
        Instant now = Instant.now();
        active = true;
        stockQuantity = 0;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void setUpdate() {
        updatedAt = Instant.now();
    }

    public void reduceStock(int quantity) {
        if (quantity <= 0) {
            throw new InvalidStockQuantityException(
                    "Quantity must be positive");
        }

        if (stockQuantity < quantity) {
            throw new InsufficientProductStockException(
                    "Insufficient product stock");
        }

        stockQuantity -= quantity;
    }
}
