package com.zenika.thezaurus.model;

import jakarta.ws.rs.core.Response;
import java.time.Instant;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {
    /**
     * Factory method utilitaire pour construire une ErrorResponse facilement.
     */
    public static ErrorResponse of(Response.Status status, String message, String path) {
        return new ErrorResponse(
                Instant.now().toString(),
                status.getStatusCode(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}