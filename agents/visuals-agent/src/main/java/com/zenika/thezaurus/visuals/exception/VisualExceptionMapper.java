package com.zenika.thezaurus.visuals.exception;

import com.zenika.thezaurus.visuals.crop.CropException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class VisualExceptionMapper implements ExceptionMapper<RuntimeException> {
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof WebApplicationException web) return web.getResponse();
        int status = exception instanceof CropException crop
                ? crop.status()
                : exception instanceof IllegalArgumentException ? 400 : 500;
        String message = status == 500 ? "Erreur interne de génération du visuel." : exception.getMessage();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", message == null ? "Requête invalide." : message))
                .build();
    }
}
