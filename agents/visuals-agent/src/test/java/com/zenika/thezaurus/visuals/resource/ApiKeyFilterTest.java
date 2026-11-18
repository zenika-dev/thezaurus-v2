package com.zenika.thezaurus.visuals.resource;

import static org.mockito.Mockito.*;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApiKeyFilterTest {
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
