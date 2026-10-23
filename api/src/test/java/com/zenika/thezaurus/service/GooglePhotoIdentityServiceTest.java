package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.auth.oauth2.TokenVerifier;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

class GooglePhotoIdentityServiceTest {
    @Test
    void acceptsOnlyVerifiedGoogleIdentityMatchingTheCurrentPerson() throws Exception {
        var verifier = mock(TokenVerifier.class);
        var payload = new JsonWebSignature.Payload();
        payload.set("email", "Jane@zenika.com");
        payload.set("email_verified", true);
        payload.set("picture", "https://lh3.googleusercontent.com/photo");
        var signature = mock(JsonWebSignature.class);
        when(signature.getPayload()).thenReturn(payload);
        when(verifier.verify("signed-token")).thenReturn(signature);
        var service = new GooglePhotoIdentityService();
        service.verifier = verifier;
        assertEquals(
                "https://lh3.googleusercontent.com/photo", service.verifiedPhoto("signed-token", "jane@zenika.com"));
        assertThrows(ForbiddenException.class, () -> service.verifiedPhoto("signed-token", "other@zenika.com"));
        payload.set("email_verified", false);
        assertThrows(ForbiddenException.class, () -> service.verifiedPhoto("signed-token", "jane@zenika.com"));
        assertThrows(ForbiddenException.class, () -> service.verifiedPhoto(null, "jane@zenika.com"));
    }

    @Test
    void rejectsInvalidSignatureOrAudience() throws Exception {
        var service = new GooglePhotoIdentityService();
        service.verifier = mock(TokenVerifier.class);
        when(service.verifier.verify("invalid"))
                .thenThrow(new TokenVerifier.VerificationException("invalid signature"));
        assertThrows(ForbiddenException.class, () -> service.verifiedPhoto("invalid", "jane@zenika.com"));
    }
}
