package com.lukete.ecommerce.dto;

import java.util.UUID;

public record ProductStockResponse(
                UUID productId,
                Integer amount) {
}
