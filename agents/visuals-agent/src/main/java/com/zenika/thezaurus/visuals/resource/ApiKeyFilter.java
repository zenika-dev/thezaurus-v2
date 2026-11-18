package com.zenika.thezaurus.visuals.resource;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Optional service credential for callers outside the local machine. */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {
    @ConfigProperty(name = "visuals.api-key")
    Optional<String> apiKey;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath().replaceFirst("^/+", "");
        if (!(path.equals("visuals") || path.startsWith("visuals/"))
                || apiKey.filter(key -> !key.isBlank()).isEmpty()) return;
        String supplied = request.getHeaderString("X-API-Key");
        if (supplied == null
                || !MessageDigest.isEqual(
                        apiKey.get().getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(Map.of("code", "UNAUTHORIZED", "message", "Clé de service invalide."))
                    .build());
        }
    }
}
