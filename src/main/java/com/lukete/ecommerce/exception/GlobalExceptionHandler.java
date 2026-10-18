package com.lukete.ecommerce.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.lukete.ecommerce.dto.ApiErrorResponse;

import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateCategory(CategoryAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "CATEGORY_ALREADY_EXISTS",
                ex.getMessage(),
                LocalDateTime.now(DEFAULT_ZONE)));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingProduct(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now(DEFAULT_ZONE)));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingCategory(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                "CATEGORY_NOT_FOUND",
                ex.getMessage(),
                LocalDateTime.now(DEFAULT_ZONE)));
    }
}