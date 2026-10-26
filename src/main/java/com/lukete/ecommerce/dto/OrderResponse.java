package com.lukete.ecommerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lukete.ecommerce.enums.OrderStatus;

public record OrderResponse(

        UUID id,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        Instant createdAt) {

    public OrderResponse {
        items = items != null ? List.copyOf(items) : List.of();
    }

    public List<OrderItemResponse> items() {
        // Retorna uma cópia imutável que protege a representação interna
        return items != null ? List.copyOf(items) : List.of();
    }
}
