package com.zenika.thezaurus.client;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Envoi d'emails via l'API HTTP Mailgun ({@code POST /v3/{domain}/messages}).
 *
 * <p>Authentification par « Domain Sending Key » (Basic {@code api:<clé>}) : ce type de clé n'est
 * accepté que sur l'API HTTP, pas en SMTP. Actif uniquement si la clé et le domaine sont
 * renseignés ; sinon l'envoi passe par quarkus-mailer (Mailpit en local).
 */
@ApplicationScoped
public class MailgunClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    @ConfigProperty(name = "thezaurus.mailgun.sending-key")
    Optional<String> sendingKey;

    @ConfigProperty(name = "thezaurus.mailgun.domain")
    Optional<String> domain;

    @ConfigProperty(name = "thezaurus.mailgun.base-url", defaultValue = "https://api.eu.mailgun.net")
    String baseUrl;

    public boolean isConfigured() {
        return sendingKey.filter(value -> !value.isBlank()).isPresent()
                && domain.filter(value -> !value.isBlank()).isPresent();
    }

    public String domain() {
        return domain.orElse("").trim();
    }

    /** Envoie un message ; lève une exception si Mailgun ne répond pas en 2xx. */
    public void send(String from, List<String> to, String subject, String html)
            throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IllegalStateException("Mailgun non configuré (clé ou domaine manquant).");
        }
        StringJoiner form = new StringJoiner("&");
        form.add(field("from", from));
        to.forEach(recipient -> form.add(field("to", recipient)));
        form.add(field("subject", subject));
        form.add(field("html", html));

        String credentials =
                Base64.getEncoder().encodeToString(("api:" + sendingKey.get().trim()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(baseUrl) + "/v3/" + domain() + "/messages"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Mailgun a répondu " + response.statusCode() + " : " + response.body());
        }
    }

    private static String field(String name, String value) {
        return name + "=" + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
