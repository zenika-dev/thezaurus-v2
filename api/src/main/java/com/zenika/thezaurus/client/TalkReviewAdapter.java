package com.zenika.thezaurus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.model.AgentPayload;
import com.zenika.thezaurus.model.TalkReviewRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Adaptateur d'infrastructure HTTP dédié aux communications avec l'agent IA Vertex AI Reasoning Engine.
 */
@ApplicationScoped
public class TalkReviewAdapter {

    private static final Logger LOG = Logger.getLogger(TalkReviewAdapter.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String reasoningEngineUrl;
    private final GoogleAuthService googleAuthService;

    @Inject
    public TalkReviewAdapter(
            ObjectMapper objectMapper,
            GoogleAuthService googleAuthService,
            @ConfigProperty(name = "thezaurus.reasoning-engine.url", defaultValue = "") String reasoningEngineUrl) {
        this.objectMapper = objectMapper;
        this.googleAuthService = googleAuthService;
        this.reasoningEngineUrl = reasoningEngineUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Envoie une requête de revue de talk à l'endpoint Vertex AI Reasoning Engine et retourne la réponse brute.
     */
    public Optional<String> sendStreamQuery(String title, String abstractText) {
        if (reasoningEngineUrl.isBlank()) {
            LOG.warn("Reasoning Engine URL is not configured.");
            return Optional.empty();
        }

        try {
            String innerMessageJson = objectMapper.writeValueAsString(new TalkReviewRequest(title, abstractText));
            String requestBody = objectMapper.writeValueAsString(AgentPayload.asyncStreamQuery(innerMessageJson));

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(reasoningEngineUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            googleAuthService.getAccessToken()
                    .ifPresent(token -> reqBuilder.header("Authorization", "Bearer " + token));

            LOG.infof("Calling Vertex AI Reasoning Engine streamQuery endpoint: %s", reasoningEngineUrl);
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOG.info("Successfully received SSE response stream from Reasoning Engine AI Agent.");
                return Optional.ofNullable(response.body());
            }

            LOG.warnf("Reasoning Engine returned HTTP %d: %s", response.statusCode(), response.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Thread interrupted while calling Reasoning Engine AI Agent", e);
        } catch (IOException e) {
            LOG.warnf("I/O error calling Reasoning Engine AI Agent: %s", e.getMessage());
        } catch (Exception e) {
            LOG.warnf("Unexpected error calling Reasoning Engine AI Agent: %s", e.getMessage(), e);
        }

        return Optional.empty();
    }
}