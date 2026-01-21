package com.mytegroup.api.exception;

/**
 * Exception thrown when the request is well-formed but semantically incorrect.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message);
    }

    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}

