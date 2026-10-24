package com.lukete.ecommerce.exception;

public class InactiveProductException extends RuntimeException {
    public InactiveProductException(String message) {
        super(message);
    }
}
