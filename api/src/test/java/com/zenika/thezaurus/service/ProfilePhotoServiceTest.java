package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfilePhotoServiceTest {
    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> googleResponse(int status, String location, byte[] bytes) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers())
                .thenReturn(HttpHeaders.of(
                        location == null ? Map.of() : Map.of("Location", List.of(location)), (name, value) -> true));
        when(response.body()).thenReturn(new ByteArrayInputStream(bytes));
        return response;
    }

    @Test
    void googlePhotoFollowsValidatedRedirectToTheActualImage() throws Exception {
        service.http = mock(HttpClient.class);
        var redirect = googleResponse(302, "https://lh7-qw.googleusercontent.com/photo", new byte[0]);
        var image = googleResponse(200, null, image("png"));
        when(service.http.send(
                        any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(redirect, image);
        var photo = service.read(user);
        assertEquals(3, ImageIO.read(new ByteArrayInputStream(photo.bytes())).getWidth());
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(service.http, times(2)).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("lh7-qw.googleusercontent.com", request.getValue().uri().getHost());
    }

    @Test
    void googlePhotoRejectsRedirectOutsideTrustedHostsBeforeFollowingIt() throws Exception {
        service.http = mock(HttpClient.class);
        var redirect = googleResponse(302, "https://untrusted.example/photo", new byte[0]);
        when(service.http.send(
                        any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(redirect);
        assertThrows(BadRequestException.class, () -> service.read(user));
        verify(service.http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void googlePhotoStopsRedirectLoops() throws Exception {
        service.http = mock(HttpClient.class);
        var redirect = googleResponse(302, "https://lh3.googleusercontent.com/loop", new byte[0]);
        when(service.http.send(
                        any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
                .thenReturn(redirect);
        var failure = assertThrows(WebApplicationException.class, () -> service.read(user));
        assertEquals(502, failure.getResponse().getStatus());
        verify(service.http, times(4)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void privatePhotoFieldsCannotBeReadOrInjectedThroughUserJson() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        User privateUser = User.builder()
                .name("Jane")
                .email("jane@zenika.com")
                .aiPhotoAllowed(true)
                .customPhotoObject("private-object")
                .googlePhotoUrl("https://lh3.googleusercontent.com/private")
                .build();
        String json = mapper.writeValueAsString(privateUser);
        assertFalse(json.contains("aiPhotoAllowed"));
        assertFalse(json.contains("customPhotoObject"));
        assertFalse(json.contains("googlePhotoUrl"));
        User injected = mapper.readValue(
                "{\"name\":\"Jane\",\"email\":\"jane@zenika.com\",\"aiPhotoAllowed\":true,\"customPhotoObject\":\"injected\"}",
                User.class);
        assertFalse(injected.allowsAiPhoto());
        assertFalse(injected.hasCustomPhoto());
    }

    ProfilePhotoService service;
    UserRepository users;
    ProfilePhotoStorage storage;
    final User user = User.builder()
            .name("Jane")
            .email("jane@zenika.com")
            .googlePhotoUrl("https://lh3.googleusercontent.com/old")
            .build();

    @BeforeEach
    void setup() {
        service = new ProfilePhotoService();
        service.users = users = mock(UserRepository.class);
        service.storage = storage = mock(ProfilePhotoStorage.class);
    }

    static byte[] image(String format) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }

    @Test
    void decodesAcceptedFormatsWithoutCropping() throws Exception {
        for (String format : List.of("png", "jpeg")) {
            var photo = ProfilePhotoService.normalize(image(format));
            var decoded = ImageIO.read(new ByteArrayInputStream(photo.bytes()));
            assertEquals(3, decoded.getWidth());
            assertEquals(2, decoded.getHeight());
            assertEquals("image/png", photo.contentType());
        }
        byte[] webp = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        assertNotNull(ImageIO.read(
                new ByteArrayInputStream(ProfilePhotoService.normalize(webp).bytes())));
    }

    @Test
    void rejectsInvalidUnsupportedAndOversizedFiles() throws Exception {
        assertThrows(BadRequestException.class, () -> ProfilePhotoService.normalize("not a photo".getBytes()));
        assertThrows(BadRequestException.class, () -> ProfilePhotoService.normalize(image("gif")));
        var error = assertThrows(
                WebApplicationException.class,
                () -> ProfilePhotoService.normalize(new byte[ProfilePhotoService.MAX_BYTES + 1]));
        assertEquals(413, error.getResponse().getStatus());
    }

    @Test
    void missingConsentFailsBeforeReadingPhoto() throws Exception {
        when(users.findByEmail(user.email())).thenReturn(user);
        assertThrows(ForbiddenException.class, () -> service.getForAgent(user.email()));
        verifyNoInteractions(storage);
    }

    @Test
    void allowedAgentGetsCustomPhotoAndRevocationAppliesOnNextCall() throws Exception {
        User allowed = user.toBuilder()
                .aiPhotoAllowed(true)
                .customPhotoObject("custom")
                .build();
        when(users.findByEmail(user.email())).thenReturn(allowed, allowed, user);
        byte[] bytes = image("png");
        when(storage.read("custom")).thenReturn(bytes);
        assertArrayEquals(bytes, service.getForAgent(user.email()).bytes());
        assertThrows(ForbiddenException.class, () -> service.getForAgent(user.email()));
        verify(storage, times(1)).read("custom");
    }

    @Test
    void revocationDuringStorageReadAlsoBlocksReturn() throws Exception {
        User allowed = user.toBuilder()
                .aiPhotoAllowed(true)
                .customPhotoObject("custom")
                .build();
        when(users.findByEmail(user.email())).thenReturn(allowed, user);
        when(storage.read("custom")).thenReturn(image("png"));
        assertThrows(ForbiddenException.class, () -> service.getForAgent(user.email()));
    }

    @Test
    void allowedButMissingPhotoIsDistinctFromRefusal() throws Exception {
        when(users.findByEmail(user.email()))
                .thenReturn(user.toBuilder()
                        .googlePhotoUrl(null)
                        .aiPhotoAllowed(true)
                        .build());
        assertThrows(NotFoundException.class, () -> service.getForAgent(user.email()));
    }

    @Test
    void customPhotoPreventsGoogleSync() throws Exception {
        service.syncGoogle(
                user.toBuilder().customPhotoObject("custom").build(), "https://lh3.googleusercontent.com/new");
        verifyNoInteractions(users);
        service.syncGoogle(user, "https://lh3.googleusercontent.com/new");
        verify(users).syncGooglePhoto(user.email(), "https://lh3.googleusercontent.com/new");
    }

    @Test
    void resetOnlyChangesCustomPhotoAndDeletesStoredObject() throws Exception {
        service.reset(user.toBuilder()
                .customPhotoObject("custom")
                .aiPhotoAllowed(true)
                .build());
        verify(users).updateCustomPhoto(user.email(), "");
        verifyNoMoreInteractions(users);
        verify(storage).delete("custom");
    }

    @Test
    void failedUploadPreservesCurrentPhotoAndCleansNewObject() throws Exception {
        when(storage.save(any(), eq("image/png"))).thenReturn("new");
        doThrow(new ExecutionException(new RuntimeException("database unavailable")))
                .when(users)
                .updateCustomPhoto(user.email(), "new");
        assertThrows(
                ExecutionException.class,
                () -> service.upload(
                        user.toBuilder().customPhotoObject("old").build(), new ByteArrayInputStream(image("png"))));
        verify(storage).delete("new");
        verify(storage, never()).delete("old");
    }

    @Test
    void userCopiesPreservePhotoAndConsent() {
        User original = user.toBuilder()
                .customPhotoObject("custom")
                .aiPhotoAllowed(true)
                .build();
        for (User copy : List.of(
                original.withName("New"), original.withRoles(List.of(Role.ADMIN)), original.withSlackUserId("U123"))) {
            assertEquals("custom", copy.customPhotoObject());
            assertEquals(user.googlePhotoUrl(), copy.googlePhotoUrl());
            assertTrue(copy.allowsAiPhoto());
        }
    }

    @Test
    void rejectsUntrustedGoogleUrls() {
        for (String url : List.of(
                "http://lh3.googleusercontent.com/photo",
                "https://localhost/photo",
                "https://lh3.googleusercontent.com.attacker.test/photo",
                "https://lh3.googleusercontent.com:444/photo")) {
            assertThrows(BadRequestException.class, () -> ProfilePhotoService.trustedGooglePhoto(url));
        }
    }
}
