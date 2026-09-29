package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Accès au compte de l'identité courante, après son authentification. */
@ApplicationScoped
public class CurrentUserService {
    @Inject
    SecurityIdentity identity;

    @Inject
    UserRepository userRepository;

    public Optional<User> getUser() throws ExecutionException, InterruptedException {
        if (identity.isAnonymous()) return Optional.empty();
        return Optional.ofNullable(
                userRepository.findByEmail(identity.getPrincipal().getName()));
    }
}
