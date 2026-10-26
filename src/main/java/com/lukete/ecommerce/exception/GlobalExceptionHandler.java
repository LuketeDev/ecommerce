package com.lukete.ecommerce.exception;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.lukete.ecommerce.dto.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryAlreadyExists(
            CategoryAlreadyExistsException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "CATEGORY_ALREADY_EXISTS",
                ex.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "CATEGORY_NOT_FOUND",
                ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(
            ProductNotFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                ex.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                ex.getMessage());
    }

    @ExceptionHandler(DuplicateOrderItemException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateOrderItem(
            DuplicateOrderItemException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "DUPLICATE_ORDER_ITEM",
                ex.getMessage());
    }

    @ExceptionHandler(InactiveProductException.class)
    public ResponseEntity<ApiErrorResponse> handleInactiveProduct(
            InactiveProductException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "INACTIVE_PRODUCT",
                ex.getMessage());
    }

    @ExceptionHandler(InsufficientProductStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientProductStock(
            InsufficientProductStockException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_PRODUCT_STOCK",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStatus(
            InvalidOrderStatusException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "INVALID_ORDER_STATUS",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidStockOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStockOperation(
            InvalidStockOperationException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_STOCK_OPERATION",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidStockQuantityException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStockQuantity(
            InvalidStockQuantityException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_STOCK_QUANTITY",
                ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "The request body is missing, malformed, or contains invalid values.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Parameter '%s' has an invalid value."
                        .formatted(ex.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Required parameter '%s' is missing."
                        .formatted(ex.getParameterName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                "The operation conflicts with existing data or a database constraint.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "The HTTP method is not supported for this endpoint.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex) {

        log.error("Unexpected error while processing request", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.");
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message) {

        ApiErrorResponse body = new ApiErrorResponse(
                error,
                message,
                LocalDateTime.now(DEFAULT_ZONE));

        return ResponseEntity.status(status).body(body);
    }
}