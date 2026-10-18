package com.lukete.ecommerce.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
                @NotBlank @Size(max = 150) String name,
                String description,
                @NotNull @Digits(integer = 10, fraction = 2) @Positive BigDecimal price,
                @NotNull UUID categoryId,
                @NotNull Boolean active) {
}
