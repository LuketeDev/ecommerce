package com.lukete.ecommerce.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
                @NotEmpty @Valid List<CreateOrderItemRequest> items) {

        public CreateOrderRequest {
                items = items != null ? List.copyOf(items) : List.of();
        }

        public List<CreateOrderItemRequest> items() {
                // Retorna uma cópia imutável que protege a representação interna
                return items != null ? List.copyOf(items) : List.of();
        }
}