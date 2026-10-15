package com.lukete.ecommerce.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String error,
        String message,
        LocalDateTime timestamp) {
}
