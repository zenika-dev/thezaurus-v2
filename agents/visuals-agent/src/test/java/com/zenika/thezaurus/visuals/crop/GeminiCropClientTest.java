package com.zenika.thezaurus.visuals.crop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.zenika.thezaurus.visuals.crop.AutoCropService.DetectionResponse;
import com.zenika.thezaurus.visuals.crop.AutoCropService.Settings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

class GeminiCropClientTest {
    private static final String NATIVE =
            "{\"subject\":{\"x\":200,\"y\":100,\"width\":600,\"height\":800},\"face\":null}";
    private final ObjectMapper mapper = new ObjectMapper();

    private Settings settings(String key, String project, String location, String model) {
        return new Settings(true, model, key, project, location, "", 3, 20000);
    }

    @SuppressWarnings("unchecked")
    private HttpClient transport(int status, String body) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(response);
        return http;
    }

    private String payload() throws Exception {
        var parts = java.util.List.of(
                java.util.Map.of("thought", true, "text", "not part of the answer"), java.util.Map.of("text", NATIVE));
        var candidate = java.util.Map.of("finishReason", "STOP", "content", java.util.Map.of("parts", parts));
        var usage = java.util.Map.of(
                "promptTokenCount", 100, "candidatesTokenCount", 40, "thoughtsTokenCount", 5, "totalTokenCount", 145);
        return mapper.writeValueAsString(
                java.util.Map.of("candidates", java.util.List.of(candidate), "usageMetadata", usage));
    }

    private HttpRequest sent(HttpClient http) throws Exception {
        ArgumentCaptor<HttpRequest> capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(capture.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        return capture.getValue();
    }

    private String body(HttpRequest request) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompletableFuture<String> done = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }

            @Override
            public void onError(Throwable error) {
                done.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                done.complete(bytes.toString(StandardCharsets.UTF_8));
            }
        });
        return done.join();
    }

    @Test
    void developerApiUsesOnlyStructuredTextAndCountsThoughtTokens() throws Exception {
        HttpClient http = transport(200, payload());
        GeminiCropClient client =
                new GeminiCropClient(settings("test-key", "", "global", "gemini-3.1-flash-lite"), mapper, http, null);
        DetectionResponse result = client.detect(new byte[] {1, 2, 3}, "");
        assertEquals(NATIVE, result.json());
        assertEquals(100, result.usage().inputTokens());
        assertEquals(45, result.usage().outputTokens());
        assertEquals(145, result.usage().totalTokens());
        HttpRequest request = sent(http);
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent",
                request.uri().toString());
        assertEquals("test-key", request.headers().firstValue("x-goog-api-key").orElseThrow());
        assertFalse(request.uri().toString().contains("test-key"));
        JsonNode json = mapper.readTree(body(request));
        assertEquals(
                "application/json",
                json.path("generationConfig").path("responseMimeType").asText());
        assertEquals(
                "MINIMAL",
                json.path("generationConfig")
                        .path("thinkingConfig")
                        .path("thinkingLevel")
                        .asText());
        assertFalse(json.path("generationConfig").has("responseModalities"));
        assertEquals(
                1000,
                json.path("generationConfig")
                        .path("responseSchema")
                        .path("properties")
                        .path("subject")
                        .path("properties")
                        .path("width")
                        .path("maximum")
                        .asInt());
        assertEquals(
                "image/jpeg",
                json.path("contents")
                        .path(0)
                        .path("parts")
                        .path(1)
                        .path("inlineData")
                        .path("mimeType")
                        .asText());
    }

    @Test
    void vertexForwardsAdcAuthorizationAndExactQuotaProject() throws Exception {
        HttpClient http = transport(200, payload());
        GoogleCredentials credentials = GoogleCredentials.newBuilder()
                .setAccessToken(new AccessToken("oauth-test-token", new Date(System.currentTimeMillis() + 3600000)))
                .setQuotaProjectId("quota-project")
                .build();
        GeminiCropClient client = new GeminiCropClient(
                settings("", "resource-project", "global", "gemini-3.1-flash-lite"), mapper, http, credentials);
        client.detect(new byte[] {1}, "");
        HttpRequest request = sent(http);
        assertEquals(
                "https://aiplatform.googleapis.com/v1/projects/resource-project/locations/global/publishers/google/models/gemini-3.1-flash-lite:generateContent",
                request.uri().toString());
        assertEquals(
                "Bearer oauth-test-token",
                request.headers().firstValue("Authorization").orElseThrow());
        assertEquals(
                "quota-project",
                request.headers().firstValue("x-goog-user-project").orElseThrow());
        assertTrue(request.headers().firstValue("x-goog-api-key").isEmpty());
    }

    @Test
    void regionalVertexAndLegacyModelUseExpectedConfiguration() throws Exception {
        HttpClient http = transport(200, payload());
        GoogleCredentials credentials =
                GoogleCredentials.create(new AccessToken("test-token", new Date(System.currentTimeMillis() + 3600000)));
        GeminiCropClient client = new GeminiCropClient(
                settings("", "resource-project", "europe-west1", "gemini-2.5-flash-lite"), mapper, http, credentials);
        client.detect(new byte[] {1}, "Coordonnées invalides.");
        HttpRequest request = sent(http);
        assertEquals("europe-west1-aiplatform.googleapis.com", request.uri().getHost());
        JsonNode json = mapper.readTree(body(request));
        assertEquals(
                0,
                json.path("generationConfig")
                        .path("thinkingConfig")
                        .path("thinkingBudget")
                        .asInt(-1));
        assertTrue(json.path("contents")
                .path(0)
                .path("parts")
                .path(0)
                .path("text")
                .asText()
                .contains("do not invent a smaller face"));
    }

    @Test
    void providerErrorsNeverExposeBodiesTokensOrCredentials() throws Exception {
        for (int status : new int[] {401, 403, 404, 429, 500}) {
            HttpClient http = transport(status, "private refresh_token=secret Bearer oauth-token");
            GeminiCropClient client = new GeminiCropClient(
                    settings("private-key", "", "global", "gemini-3.1-flash-lite"), mapper, http, null);
            CropException error = assertThrows(CropException.class, () -> client.detect(new byte[] {1}, ""));
            assertTrue(error.status() >= 500);
            assertNull(error.getCause());
            assertFalse(error.getMessage().contains("secret"));
            assertFalse(error.getMessage().contains("private-key"));
            assertFalse(error.getMessage().contains("oauth-token"));
            verify(http, times(1))
                    .send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        }
    }

    @Test
    void safetyBlockDoesNotBecomeAFormattingRetry() throws Exception {
        HttpClient http = transport(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        GeminiCropClient client =
                new GeminiCropClient(settings("test", "", "global", "gemini-3.1-flash-lite"), mapper, http, null);
        CropException error = assertThrows(CropException.class, () -> client.detect(new byte[] {1}, ""));
        assertEquals(422, error.status());
        assertEquals("PROVIDER_BLOCKED", error.code());
    }

    @Test
    void timeoutAndMissingCredentialsProduceSafeFailures() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new IOException("private transport details"));
        GeminiCropClient client =
                new GeminiCropClient(settings("test", "", "global", "gemini-3.1-flash-lite"), mapper, http, null);
        CropException error = assertThrows(CropException.class, () -> client.detect(new byte[] {1}, ""));
        assertEquals("PROVIDER_UNAVAILABLE", error.code());
        assertFalse(error.getMessage().contains("private"));
        Settings missing = new Settings(
                true, "gemini-3.1-flash-lite", "", "project-test", "global", "missing-credential-file.json", 3, 20000);
        GeminiCropClient withoutCredentials = new GeminiCropClient(missing, mapper, http, null);
        assertEquals(
                "PROVIDER_AUTH",
                assertThrows(CropException.class, () -> withoutCredentials.detect(new byte[] {1}, ""))
                        .code());
    }
}
