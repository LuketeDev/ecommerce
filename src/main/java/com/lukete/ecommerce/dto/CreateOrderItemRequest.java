package com.lukete.ecommerce.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity) {

}
