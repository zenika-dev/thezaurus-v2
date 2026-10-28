package com.zenika.thezaurus.visuals.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class VisualExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();
        }
        if (exception instanceof VisualGenerationException) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", exception.getMessage() != null ? exception.getMessage() : "Erreur interne"))
                .build();
    }
}
