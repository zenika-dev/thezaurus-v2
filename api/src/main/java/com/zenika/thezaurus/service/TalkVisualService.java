package com.zenika.thezaurus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TalkVisualService {
    @Inject
    TalkService talks;

    @Inject
    ProfilePhotoService photos;

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "visuals.agent.url", defaultValue = "")
    Optional<String> agentUrl;

    @ConfigProperty(name = "visuals.agent.api-key", defaultValue = "")
    Optional<String> apiKey;

    final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public byte[] generate(String id) throws ExecutionException, InterruptedException {
        if (agentUrl.isEmpty() || agentUrl.get().isBlank())
            throw new WebApplicationException("Agent visuel non configuré.", 503);
        var talk = talks.findById(id);
        if (talk == null) throw new NotFoundException();
        if (talk.speakers() == null
                || talk.speakers().size() != 1
                || talk.conference() == null
                || talk.conference().getName() == null
                || talk.conference().getName().isBlank()) {
            throw new BadRequestException("Un seul intervenant et une conférence sont requis.");
        }
        var speaker = talk.speakers().getFirst();
        if (speaker.email() == null || speaker.email().isBlank()) {
            throw new BadRequestException("L’intervenant doit avoir un profil avec une photo autorisée pour l’IA.");
        }
        var photo = photos.getForAgent(speaker.email());
        try {
            var body = Map.of(
                    "title",
                    talk.title(),
                    "conference",
                    talk.conference().getName(),
                    "speakers",
                    List.of(Map.of(
                            "name",
                            speaker.name(),
                            "photo",
                            Map.of("base64", Base64.getEncoder().encodeToString(photo.bytes())))),
                    "autoCrop",
                    true);
            var request = HttpRequest.newBuilder(URI.create(agentUrl.orElseThrow() + "/visuals/talks/image"))
                    .timeout(Duration.ofSeconds(120))
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
