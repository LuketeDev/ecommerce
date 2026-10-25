package com.lukete.ecommerce.exception;

public class DuplicateOrderItemException extends RuntimeException {
    public DuplicateOrderItemException(String message) {
        super(message);
    }
}
