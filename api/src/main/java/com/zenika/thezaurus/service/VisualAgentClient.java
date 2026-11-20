package com.zenika.thezaurus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class VisualAgentClient {
    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "visuals.agent.url", defaultValue = "")
    Optional<String> agentUrl;

    @ConfigProperty(name = "visuals.agent.api-key", defaultValue = "")
    Optional<String> apiKey;

    @ConfigProperty(name = "visuals.agent.timeout", defaultValue = "PT120S")
    Duration timeout = Duration.ofSeconds(120);

    final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public void requireConfigured() {
        if (agentUrl.isEmpty() || agentUrl.get().isBlank())
            throw new WebApplicationException("Agent visuel non configuré.", 503);
    }

    public byte[] generate(String category, VisualAgentRequest body) throws InterruptedException {
        requireConfigured();
        try {
            var request = HttpRequest.newBuilder(URI.create(agentUrl.orElseThrow() + "/visuals/" + category + "/image"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "image/png")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));
            apiKey.filter(key -> !key.isBlank()).ifPresent(key -> request.header("X-API-Key", key));
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200
                    || !response.headers().firstValue("Content-Type").orElse("").startsWith("image/png")) {
                throw new WebApplicationException("La génération du visuel a échoué. Vous pouvez réessayer.", 502);
            }
            return response.body();
        } catch (IOException e) {
            throw new WebApplicationException("L’agent visuel est indisponible. Vous pouvez réessayer.", 502);
        }
    }
}
