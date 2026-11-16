package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.User;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TalkVisualServiceTest {
    private final TalkVisualService service = new TalkVisualService();

    private void setup(int speakers) throws Exception {
        service.talks = mock(TalkService.class);
        service.photos = mock(ProfilePhotoService.class);
        service.json = new ObjectMapper();
        service.agentUrl = java.util.Optional.of("http://localhost:1");
        service.apiKey = java.util.Optional.empty();
        var talk = mock(Talk.class);
        var conference = mock(Conference.class);
        when(service.talks.findById("talk")).thenReturn(talk);
        when(talk.speakers())
                .thenReturn(java.util.Collections.nCopies(
                        speakers,
                        User.builder().name("Alice").email("alice@example.com").build()));
        if (speakers == 1) {
            when(talk.conference()).thenReturn(conference);
            when(conference.getName()).thenReturn("Devoxx");
            when(talk.title()).thenReturn("Mon talk");
        }
    }

    @Test
    void refusesMultipleSpeakersBeforeReadingPhotos() throws Exception {
        setup(2);
        assertThrows(BadRequestException.class, () -> service.generate("talk"));
        verifyNoInteractions(service.photos);
    }

    @Test
    void enforcesPhotoConsentBeforeCallingAgent() throws Exception {
        setup(1);
        when(service.photos.getForAgent("alice@example.com")).thenThrow(new ForbiddenException());
        assertThrows(ForbiddenException.class, () -> service.generate("talk"));
    }

    @Test
    void sendsSavedTalkAndAuthorizedPhotoAndReturnsPng() throws Exception {
        setup(1);
        when(service.photos.getForAgent("alice@example.com"))
                .thenReturn(new ProfilePhotoService.Photo(new byte[] {1, 2, 3}, "image/png"));
        var body = new AtomicReference<String>();
        var key = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/visuals/talks/image", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, 3);
            exchange.getResponseBody().write(new byte[] {4, 5, 6});
            exchange.close();
        });
        server.start();
        try {
            service.agentUrl = java.util.Optional.of(
                    "http://127.0.0.1:" + server.getAddress().getPort());
            service.apiKey = java.util.Optional.of("test-key");
            assertArrayEquals(new byte[] {4, 5, 6}, service.generate("talk"));
            var request = service.json.readTree(body.get());
            assertEquals("Mon talk", request.path("title").asText());
            assertEquals("Devoxx", request.path("conference").asText());
            assertEquals(
                    "AQID",
                    request.path("speakers").get(0).path("photo").path("base64").asText());
            assertTrue(request.path("autoCrop").asBoolean());
            assertEquals("test-key", key.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unavailableAgentReturnsGatewayError() throws Exception {
        setup(1);
        when(service.photos.getForAgent("alice@example.com"))
                .thenReturn(new ProfilePhotoService.Photo(new byte[] {1}, "image/png"));
        assertEquals(
                502,
                assertThrows(WebApplicationException.class, () -> service.generate("talk"))
                        .getResponse()
                        .getStatus());
    }
}
