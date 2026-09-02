package com.zenika.thezaurus.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TalkReviewAdapterTest {

    private static final String TEST_URL =
            "https://europe-west1-aiplatform.googleapis.com/v1/projects/test/locations/europe-west1/reasoningEngines/123:streamQuery?alt=sse";

    private ObjectMapper objectMapper;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private TalkReviewAdapter talkReviewAdapter;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        talkReviewAdapter = new TalkReviewAdapter(objectMapper, googleAuthService, TEST_URL);

        Field httpClientField = TalkReviewAdapter.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(talkReviewAdapter, mockHttpClient);
    }

    @Test
    @DisplayName("sendStreamQuery - avec URL non configurée (vide) - retourne Optional.empty")
    void sendStreamQuery_WithBlankUrl_ReturnsEmptyOptional() {
        TalkReviewAdapter adapterWithBlankUrl = new TalkReviewAdapter(objectMapper, googleAuthService, "   ");

        Optional<String> result = adapterWithBlankUrl.sendStreamQuery("Titre", "Abstract");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("sendStreamQuery - réponse HTTP 200 avec token OAuth2 - retourne le corps de réponse")
    @SuppressWarnings("unchecked")
    void sendStreamQuery_Http200_ReturnsResponseBody() throws Exception {
        String expectedResponseBody = "data: {\"improvements\": []}";

        when(googleAuthService.getAccessToken()).thenReturn(Optional.of("test-bearer-token"));
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        Optional<String> result = talkReviewAdapter.sendStreamQuery("Titre de talk", "Abstract du talk");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(expectedResponseBody, result.get());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any());

        HttpRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertEquals(TEST_URL, capturedRequest.uri().toString());
        Assertions.assertEquals("POST", capturedRequest.method());
        Assertions.assertTrue(
                capturedRequest.headers().firstValue("Authorization").isPresent());
        Assertions.assertEquals(
                "Bearer test-bearer-token",
                capturedRequest.headers().firstValue("Authorization").get());
    }

    @Test
    @DisplayName(
            "sendStreamQuery - réponse HTTP 200 sans token OAuth2 - effectue la requête sans en-tête Authorization")
    @SuppressWarnings("unchecked")
    void sendStreamQuery_Http200WithoutToken_ReturnsResponseBody() throws Exception {
        String expectedResponseBody = "data: {\"improvements\": []}";

        when(googleAuthService.getAccessToken()).thenReturn(Optional.empty());
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(expectedResponseBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        Optional<String> result = talkReviewAdapter.sendStreamQuery("Titre", "Abstract");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(expectedResponseBody, result.get());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any());

        HttpRequest capturedRequest = requestCaptor.getValue();
        Assertions.assertTrue(
                capturedRequest.headers().firstValue("Authorization").isEmpty());
    }

    @Test
    @DisplayName("sendStreamQuery - erreur HTTP 500 - retourne Optional.empty")
    @SuppressWarnings("unchecked")
    void sendStreamQuery_Http500_ReturnsEmptyOptional() throws Exception {
        when(googleAuthService.getAccessToken()).thenReturn(Optional.of("token"));
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("Internal Server Error");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        Optional<String> result = talkReviewAdapter.sendStreamQuery("Titre", "Abstract");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("sendStreamQuery - exception d'E/S (IOException) - gère l'erreur et retourne Optional.empty")
    @SuppressWarnings("unchecked")
    void sendStreamQuery_IOException_ReturnsEmptyOptional() throws Exception {
        when(googleAuthService.getAccessToken()).thenReturn(Optional.of("token"));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection reset by peer"));

        Optional<String> result = talkReviewAdapter.sendStreamQuery("Titre", "Abstract");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName(
            "sendStreamQuery - interruption du thread (InterruptedException) - réinterrompt le thread et retourne Optional.empty")
    @SuppressWarnings("unchecked")
    void sendStreamQuery_InterruptedException_ReturnsEmptyOptional() throws Exception {
        when(googleAuthService.getAccessToken()).thenReturn(Optional.of("token"));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Task interrupted"));

        Optional<String> result = talkReviewAdapter.sendStreamQuery("Titre", "Abstract");

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertTrue(Thread.currentThread().isInterrupted());
        // Clean up interrupt status for subsequent tests
        Thread.interrupted();
    }
}
