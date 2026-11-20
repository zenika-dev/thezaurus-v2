package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.WebApplicationException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VisualAgentClientTest {
    @Test
    void configuredDeadlineStopsAnUnresponsiveAgent() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/visuals/blogs/image", exchange -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        try {
            var client = new VisualAgentClient();
            client.json = new ObjectMapper();
            client.agentUrl =
                    Optional.of("http://127.0.0.1:" + server.getAddress().getPort());
            client.apiKey = Optional.empty();
            client.timeout = Duration.ofMillis(50);
            var request =
                    new VisualAgentRequest.Blog("Titre", List.of(new VisualAgentRequest.Person("Alice", null)), true);
            var error = assertThrows(WebApplicationException.class, () -> client.generate("blogs", request));
            assertEquals(502, error.getResponse().getStatus());
        } finally {
            server.stop(0);
        }
    }
}
