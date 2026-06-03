package com.zenika.thezaurus.auth;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;

@ApplicationScoped
public class IapSecurityAugmentor implements SecurityIdentityAugmentor {

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "mock.auth", defaultValue = "false")
    boolean mockAuth;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        System.out.println("====== AUGMENT CALLED ======");
        System.out.println("Mock Auth: " + mockAuth);
        System.out.println("Is Anonymous: " + identity.isAnonymous());
        // Bypass conditionnel de l'authentification
        if (mockAuth && identity.isAnonymous()) {
            System.out.println("Creating dev identity!");
            SecurityIdentity devIdentity = QuarkusSecurityIdentity.builder(identity)
                    .setPrincipal(() -> "dev@zenika.com")
                    .addRole("admin")
                    .addRole("membre")
                    .build();
            return Uni.createFrom().item(devIdentity);
        }

        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        // Si l'identité provient du JWT (IAP)
        if (identity.getPrincipal() instanceof JsonWebToken) {
            JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
            String email = jwt.getClaim("email");

            if (email != null && email.endsWith("@zenika.com")) {
                return Uni.createFrom().item(() -> enrichIdentity(identity, email));
            } else {
                // Email invalide ou non Zenika : on ne donne pas de rôles
                return Uni.createFrom().item(identity);
            }
        }

        return Uni.createFrom().item(identity);
    }

    private SecurityIdentity enrichIdentity(SecurityIdentity identity, String email) {
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                // Création auto avec rôle par défaut 'membre'
                user = new User(email, "membre");
                userRepository.create(user);
            }

            return QuarkusSecurityIdentity.builder(identity)
                    .setPrincipal(() -> email)
                    .addRole(user.getRole())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Erreur d'accès à Firestore pour l'utilisateur", e);
        }
    }
}
