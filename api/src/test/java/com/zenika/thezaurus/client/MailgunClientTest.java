package com.zenika.thezaurus.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MailgunClientTest {

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private int status = 200;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"message\":\"Queued\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private MailgunClient client(String key, String domain) {
        MailgunClient client = new MailgunClient();
        client.sendingKey = Optional.ofNullable(key);
        client.domain = Optional.ofNullable(domain);
        client.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        return client;
    }

    @Test
    void postsTheMessageToTheDomainEndpointWithBasicAuth() throws Exception {
        client("key-123", "m.example.com")
                .send(
                        "noreply@m.example.com",
                        List.of("alice@example.com", "bob@example.com"),
                        "Rappel « Java »",
                        "<p>Bonjour & merci</p>");

        assertEquals("/v3/m.example.com/messages", path.get());
        String expectedAuth =
                "Basic " + Base64.getEncoder().encodeToString("api:key-123".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedAuth, authorization.get());

        List<String> fields = new ArrayList<>();
        for (String pair : body.get().split("&")) {
            fields.add(URLDecoder.decode(pair, StandardCharsets.UTF_8));
        }
        assertEquals(
                List.of(
                        "from=noreply@m.example.com",
                        "to=alice@example.com",
                        "to=bob@example.com",
                        "subject=Rappel « Java »",
                        "html=<p>Bonjour & merci</p>"),
                fields);
    }

    @Test
    void failsWhenMailgunRejectsTheMessage() {
        status = 401;
        var client = client("bad-key", "m.example.com");
        var error = assertThrows(
                IOException.class, () -> client.send("noreply@m.example.com", List.of("a@example.com"), "s", "h"));
        assertTrue(error.getMessage().contains("401"));
    }

    @Test
    void isOnlyConfiguredWithBothKeyAndDomain() {
        assertTrue(client("key", "m.example.com").isConfigured());
        assertFalse(client("", "m.example.com").isConfigured());
        assertFalse(client("key", " ").isConfigured());
        assertFalse(client(null, null).isConfigured());
    }
}
