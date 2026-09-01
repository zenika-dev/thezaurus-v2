package com.zenika.thezaurus.client;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GoogleAuthService {

    private static final Logger LOG = Logger.getLogger(GoogleAuthService.class);
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/cloud-platform");

    public Optional<String> getAccessToken() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            if (credentials.createScopedRequired()) {
                credentials = credentials.createScoped(SCOPES);
            }
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() != null) {
                return Optional.ofNullable(credentials.getAccessToken().getTokenValue());
            }
        } catch (IOException e) {
            LOG.warnf("Could not retrieve Google Application Default Credentials: %s", e.getMessage());
        } catch (Exception e) {
            LOG.warnf("Unexpected error during Google authentication: %s", e.getMessage(), e);
        }
        return Optional.empty();
    }
}