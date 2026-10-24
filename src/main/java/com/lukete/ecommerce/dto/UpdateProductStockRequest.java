package com.lukete.ecommerce.dto;

import com.lukete.ecommerce.enums.OperationType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateProductStockRequest(
        @NotNull @Positive Integer amount,
        @NotNull OperationType operationType) {
}
