package com.zenika.thezaurus.visuals.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApiKeyFilterTest {
    private static ApiKeyFilter clients(String value) {
        var filter = new ApiKeyFilter();
        filter.apiKey = Optional.empty();
        filter.apiKeysJson = Optional.of(value);
        filter.json = new ObjectMapper();
        filter.requireApiKey = true;
        filter.configureKeys();
        return filter;
    }

    @Test
    void acceptsIndependentClientsAndRejectsUnknownOrMissingKeys() {
        var filter = clients("{\"thezaurus\":\"first-key\",\"external\":\"second-key\"}");
        for (var key : new String[] {"first-key", "second-key", "unknown", null}) {
            var request = request("/visuals/blogs/image", key);
            filter.filter(request);
            if ("first-key".equals(key) || "second-key".equals(key))
                verify(request, never()).abortWith(any());
            else verify(request).abortWith(argThat(response -> response.getStatus() == 401));
        }
    }

    @Test
    void revokingOneClientPreservesOtherClients() {
        var filter = clients("{\"external\":\"second-key\"}");
        var revoked = request("visuals/talks/image", "first-key");
        filter.filter(revoked);
        verify(revoked).abortWith(argThat(response -> response.getStatus() == 401));
        var active = request("visuals/talks/image", "second-key");
        filter.filter(active);
        verify(active, never()).abortWith(any());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "[]",
                "null",
                "{",
                "{\"app\":\" \"}",
                "{\"app\":42}",
                "{\"\":\"secret\"}",
                "{\"a\":\"same\",\"b\":\"same\"}"
            })
    void invalidClientConfigurationFailsWithoutLeakingSecrets(String value) {
        var error = assertThrows(IllegalStateException.class, () -> clients(value));
        assertEquals("Configuration des clés API de l’agent invalide.", error.getMessage());
        assertNull(error.getCause());
    }

    @Test
    void productionCannotStartWithoutKeys() {
        var filter = new ApiKeyFilter();
        filter.apiKey = Optional.empty();
        filter.requireApiKey = true;
        assertThrows(IllegalStateException.class, filter::configureKeys);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"visuals", "/visuals", "visuals/talks/image", "/visuals/blogs/image", "//visuals/templates"})
    void protectsEveryVisualPath(String path) {
        for (String supplied : new String[] {null, "", "wrong", "secret"}) {
            var filter = new ApiKeyFilter();
            filter.apiKey = Optional.of("secret");
            var request = request(path, supplied);
            filter.filter(request);
            if ("secret".equals(supplied)) verify(request, never()).abortWith(any());
            else verify(request).abortWith(argThat(response -> response.getStatus() == 401));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "secret"})
    void leavesHealthOutsideCredentialProtection(String key) {
        var filter = new ApiKeyFilter();
        filter.apiKey = Optional.of(key);
        var request = request("/q/health", null);
        filter.filter(request);
        verify(request, never()).abortWith(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void blankOrAbsentConfigurationKeepsLocalMode(String key) {
        var filter = new ApiKeyFilter();
        for (var configured : java.util.List.of(Optional.<String>empty(), Optional.of(key))) {
            filter.apiKey = configured;
            var request = request("/visuals/talks/image", null);
            filter.filter(request);
            verify(request, never()).abortWith(any());
        }
    }

    private static ContainerRequestContext request(String path, String supplied) {
        var request = mock(ContainerRequestContext.class);
        var uri = mock(UriInfo.class);
        when(request.getUriInfo()).thenReturn(uri);
        when(uri.getPath()).thenReturn(path);
        when(request.getHeaderString("X-API-Key")).thenReturn(supplied);
        return request;
    }
}
