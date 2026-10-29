package com.zenika.thezaurus.service;

import com.google.auth.oauth2.TokenVerifier;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Vérifie le token Google même quand l’authentification de l’API est assurée par IAP. */
@ApplicationScoped
public class GooglePhotoIdentityService {
    @ConfigProperty(name = "thezaurus.photos.google-client-id")
    String clientId;

    TokenVerifier verifier;

    @PostConstruct
    void initialize() {
        verifier = TokenVerifier.newBuilder()
                .setAudience(clientId)
                .setIssuer("https://accounts.google.com")
                .build();
    }

    public String verifiedPhoto(String idToken, String currentEmail) {
        if (idToken == null || idToken.isBlank()) throw new ForbiddenException();
        try {
            var payload = verifier.verify(idToken).getPayload();
            if (!(payload.get("email") instanceof String email)
                    || !currentEmail.equalsIgnoreCase(email)
                    || !Boolean.TRUE.equals(payload.get("email_verified"))) throw new ForbiddenException();
            return payload.get("picture") instanceof String picture ? picture : null;
        } catch (TokenVerifier.VerificationException e) {
            throw new ForbiddenException("Identité Google invalide.");
        }
    }
}
