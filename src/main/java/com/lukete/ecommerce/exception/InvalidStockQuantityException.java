package com.lukete.ecommerce.exception;

public class InvalidStockQuantityException extends RuntimeException {
    public InvalidStockQuantityException(String message) {
        super(message);
    }
}
