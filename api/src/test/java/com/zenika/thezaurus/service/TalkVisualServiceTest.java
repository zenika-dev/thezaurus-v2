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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
                .thenReturn(java.util.stream.IntStream.range(0, speakers)
                        .mapToObj(index -> User.builder()
                                .name("Speaker " + index)
                                .email(email(index))
                                .build())
                        .toList());
        if (speakers >= 1 && speakers <= 3) {
            when(talk.conference()).thenReturn(conference);
            when(conference.getName()).thenReturn("Devoxx");
            when(talk.title()).thenReturn("Mon talk");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    void refusesUnsupportedSpeakerCountsBeforeReadingPhotos(int count) throws Exception {
        setup(count);
        assertThrows(BadRequestException.class, () -> service.generate("talk"));
        verifyNoInteractions(service.photos);
    }

    @Test
    void enforcesPhotoConsentBeforeCallingAgent() throws Exception {
        setup(1);
        when(service.photos.getForAgent("alice@example.com")).thenThrow(new ForbiddenException());
        assertThrows(ForbiddenException.class, () -> service.generate("talk"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void sendsSavedTalkAndAuthorizedPhotosInOrderAndReturnsPng(int count) throws Exception {
        setup(count);
        for (int i = 0; i < count; i++) {
            when(service.photos.getForAgent(email(i)))
                    .thenReturn(new ProfilePhotoService.Photo(new byte[] {(byte) i, 2, 3}, "image/png"));
        }
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
            assertEquals(count, request.path("speakers").size());
            for (int i = 0; i < count; i++) {
                var speaker = request.path("speakers").get(i);
                assertEquals("Speaker " + i, speaker.path("name").asText());
                assertEquals(
                        java.util.Base64.getEncoder().encodeToString(new byte[] {(byte) i, 2, 3}),
                        speaker.path("photo").path("base64").asText());
            }
            assertTrue(request.path("autoCrop").asBoolean());
            assertEquals("test-key", key.get());
        } finally {
            server.stop(0);
        }
    }

    private static String email(int index) {
        return index == 0 ? "alice@example.com" : "speaker" + index + "@example.com";
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void refusesGenerationIfLastSpeakerHasNotAuthorizedTheirPhoto(int count) throws Exception {
        setup(count);
        for (int i = 0; i < count - 1; i++) {
            when(service.photos.getForAgent(email(i)))
                    .thenReturn(new ProfilePhotoService.Photo(new byte[] {1}, "image/png"));
        }
        when(service.photos.getForAgent(email(count - 1))).thenThrow(new ForbiddenException());
        assertThrows(ForbiddenException.class, () -> service.generate("talk"));
        for (int i = 0; i < count; i++) verify(service.photos).getForAgent(email(i));
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
