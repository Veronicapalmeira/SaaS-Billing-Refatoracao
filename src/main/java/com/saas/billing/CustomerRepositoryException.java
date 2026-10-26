package com.saas.billing;

public class CustomerRepositoryException extends RuntimeException {

    public CustomerRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
