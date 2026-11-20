package com.zenika.thezaurus.visuals.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Optional service credential for callers outside the local machine. */
@Provider
@Startup
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {
    @ConfigProperty(name = "visuals.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "visuals.api-keys-json")
    Optional<String> apiKeysJson = Optional.empty();

    @ConfigProperty(name = "visuals.require-api-key", defaultValue = "false")
    boolean requireApiKey;

    @Inject
    ObjectMapper json;

    private List<String> clientKeys = List.of();

    @PostConstruct
    void configureKeys() {
        if (apiKeysJson.isPresent()) {
            try {
                var clients = json.readTree(apiKeysJson.get());
                if (!clients.isObject() || clients.isEmpty()) throw new IllegalArgumentException();
                var keys = new ArrayList<String>();
                var entries = clients.fields();
                while (entries.hasNext()) {
                    var entry = entries.next();
                    if (entry.getKey().isBlank()
                            || !entry.getValue().isTextual()
                            || entry.getValue().asText().isBlank()) throw new IllegalArgumentException();
                    keys.add(entry.getValue().asText());
                }
                if (keys.stream().distinct().count() != keys.size()) throw new IllegalArgumentException();
                clientKeys = List.copyOf(keys);
            } catch (Exception invalid) {
                // Do not expose parser exceptions, which may contain secret values.
                throw new IllegalStateException("Configuration des clés API de l’agent invalide.");
            }
        }
        if (requireApiKey
                && clientKeys.isEmpty()
                && apiKey.filter(key -> !key.isBlank()).isEmpty())
            throw new IllegalStateException("Au moins une clé API est requise pour cet agent.");
    }

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath().replaceFirst("^/+", "");
        if (!(path.equals("visuals") || path.startsWith("visuals/"))) return;
        var keys = new ArrayList<>(clientKeys);
        apiKey.filter(key -> !key.isBlank()).ifPresent(keys::add);
        if (keys.isEmpty()) return;
        String supplied = request.getHeaderString("X-API-Key");
        boolean accepted = false;
        for (var key : keys) {
            accepted |= supplied != null
                    && MessageDigest.isEqual(
                            key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        }
        if (!accepted) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(Map.of("code", "UNAUTHORIZED", "message", "Clé de service invalide."))
                    .build());
        }
    }
}
