package com.zenika.thezaurus.exception;

import jakarta.ws.rs.core.Response;

/**
 * ThezaurusException : class de base des exceptions dont toutes les exceptions hériterons
 */
public class ThezaurusException extends RuntimeException {

    private final Response.Status status;

    public ThezaurusException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public ThezaurusException(String message, Throwable cause, Response.Status status) {
        super(message, cause);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
