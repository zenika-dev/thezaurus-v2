package com.zenika.thezaurus.exception;

import com.zenika.thezaurus.model.ErrorResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ThezaurusExceptionMapper implements ExceptionMapper<ThezaurusException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ThezaurusException exception) {
        Response.Status status = exception.getStatus();

        ErrorResponse payload = ErrorResponse.of(
                status,
                exception.getMessage(),
                uriInfo.getPath());

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}
