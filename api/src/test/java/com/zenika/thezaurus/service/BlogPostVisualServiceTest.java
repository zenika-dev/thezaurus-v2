package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.User;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlogPostVisualServiceTest {
    private final BlogPostVisualService service = new BlogPostVisualService();

    private void setup(int speakers) throws Exception {
        service.posts = mock(BlogPostService.class);
        service.agent = new VisualAgentClient();
        service.participants = new VisualParticipantsService();
        service.participants.photos = mock(ProfilePhotoService.class);
        when(service.participants.photos.requireUser(anyString()))
                .thenAnswer(invocation -> User.builder()
                        .email(invocation.getArgument(0))
                        .aiPhotoAllowed(true)
                        .build());
        service.agent.json = new ObjectMapper();
        service.agent.agentUrl = java.util.Optional.of("http://localhost:1");
        service.agent.apiKey = java.util.Optional.empty();
        var talk = mock(BlogPost.class);
        when(service.posts.findById("talk")).thenReturn(talk);
        when(talk.getWriters())
                .thenReturn(java.util.stream.IntStream.range(0, speakers)
                        .mapToObj(index -> User.builder()
                                .name("Speaker " + index)
                                .email(email(index))
                                .build())
                        .toList());
        if (speakers >= 1 && speakers <= 2) {
            when(talk.getTitle()).thenReturn("Mon talk");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    void refusesUnsupportedSpeakerCountsBeforeReadingPhotos(int count) throws Exception {
        setup(count);
        assertThrows(BadRequestException.class, () -> service.generate("talk"));
        verifyNoInteractions(service.participants.photos);
    }

    @Test
    void generatesWithPlaceholderWhenOnlyPersonDeclinesPhoto() throws Exception {
        setup(1);
        service.agent = mock(VisualAgentClient.class);
        when(service.agent.generate(eq("blogs"), any(VisualAgentRequest.class))).thenReturn(new byte[] {4, 5, 6});
        when(service.participants.photos.getForAgent("alice@example.com")).thenThrow(new ForbiddenException());
        assertArrayEquals(new byte[] {4, 5, 6}, service.generate("talk"));
        var body = org.mockito.ArgumentCaptor.forClass(VisualAgentRequest.class);
        verify(service.agent).generate(eq("blogs"), body.capture());
        var request = new ObjectMapper().valueToTree(body.getValue());
        assertEquals("Speaker 0", request.path("writers").get(0).path("name").asText());
        assertFalse(request.path("writers").get(0).has("photo"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void sendsSavedTalkAndAuthorizedPhotosInOrderAndReturnsPng(int count) throws Exception {
        setup(count);
        for (int i = 0; i < count; i++) {
            when(service.participants.photos.getForAgent(email(i)))
                    .thenReturn(new ProfilePhotoService.Photo(new byte[] {(byte) i, 2, 3}, "image/png"));
        }
        var body = new AtomicReference<String>();
        var key = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/visuals/blogs/image", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, 3);
            exchange.getResponseBody().write(new byte[] {4, 5, 6});
            exchange.close();
        });
        server.start();
        try {
            service.agent.agentUrl = java.util.Optional.of(
                    "http://127.0.0.1:" + server.getAddress().getPort());
            service.agent.apiKey = java.util.Optional.of("test-key");
            assertArrayEquals(new byte[] {4, 5, 6}, service.generate("talk"));
            var request = service.agent.json.readTree(body.get());
            assertEquals("Mon talk", request.path("title").asText());
            assertFalse(request.has("conference"));
            assertEquals(count, request.path("writers").size());
            for (int i = 0; i < count; i++) {
                var speaker = request.path("writers").get(i);
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

    @Test
    void generatesForFreeAuthorWithoutProfile() throws Exception {
        setup(1);
        var post = service.posts.findById("talk");
        when(post.getWriters())
                .thenReturn(
                        java.util.List.of(User.builder().name("Auteur libre").build()));
        service.agent = mock(VisualAgentClient.class);
        when(service.agent.generate(eq("blogs"), any(VisualAgentRequest.class))).thenReturn(new byte[] {1});
        assertArrayEquals(new byte[] {1}, service.generate("talk"));
        var body = org.mockito.ArgumentCaptor.forClass(VisualAgentRequest.class);
        verify(service.agent).generate(eq("blogs"), body.capture());
        var author = ((VisualAgentRequest.Blog) body.getValue()).writers().getFirst();
        assertEquals("Auteur libre", author.name());
        assertNull(author.photo());
        verifyNoInteractions(service.participants.photos);
    }

    private static String email(int index) {
        return index == 0 ? "alice@example.com" : "speaker" + index + "@example.com";
    }

    @ParameterizedTest
    @ValueSource(ints = {2})
    void usesPlaceholderOnlyForLastPersonWhoDeclinedPhoto(int count) throws Exception {
        setup(count);
        for (int i = 0; i < count - 1; i++) {
            when(service.participants.photos.getForAgent(email(i)))
                    .thenReturn(new ProfilePhotoService.Photo(new byte[] {1}, "image/png"));
        }
        when(service.participants.photos.getForAgent(email(count - 1))).thenThrow(new ForbiddenException());
        service.agent = mock(VisualAgentClient.class);
        when(service.agent.generate(eq("blogs"), any(VisualAgentRequest.class))).thenReturn(new byte[] {4, 5, 6});
        assertArrayEquals(new byte[] {4, 5, 6}, service.generate("talk"));
        var body = org.mockito.ArgumentCaptor.forClass(VisualAgentRequest.class);
        verify(service.agent).generate(eq("blogs"), body.capture());
        var request = new ObjectMapper().valueToTree(body.getValue());
        assertEquals(count, request.path("writers").size());
        for (int i = 0; i < count; i++) {
            assertEquals(i < count - 1, request.path("writers").get(i).has("photo"));
            assertEquals(
                    "Speaker " + i, request.path("writers").get(i).path("name").asText());
        }
        for (int i = 0; i < count; i++) verify(service.participants.photos).getForAgent(email(i));
    }

    @Test
    void unavailableAgentReturnsGatewayError() throws Exception {
        setup(1);
        when(service.participants.photos.getForAgent("alice@example.com"))
                .thenReturn(new ProfilePhotoService.Photo(new byte[] {1}, "image/png"));
        assertEquals(
                502,
                assertThrows(WebApplicationException.class, () -> service.generate("talk"))
                        .getResponse()
                        .getStatus());
    }
}
