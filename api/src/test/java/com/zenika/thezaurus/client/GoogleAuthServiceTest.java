package com.zenika.thezaurus.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class GoogleAuthServiceTest {

    @Test
    @DisplayName("getAccessToken - environnement sans Google Application Default Credentials - retourne Optional.empty sans levée d'exception")
    void getAccessToken_WithoutAdcEnvironment_ReturnsEmptyOptional() {
        GoogleAuthService googleAuthService = new GoogleAuthService();

        Optional<String> token = Assertions.assertDoesNotThrow(googleAuthService::getAccessToken);

        Assertions.assertNotNull(token);
    }
}
