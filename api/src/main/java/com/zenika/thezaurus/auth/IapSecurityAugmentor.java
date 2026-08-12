package com.zenika.thezaurus.auth;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;

@ApplicationScoped
public class IapSecurityAugmentor implements SecurityIdentityAugmentor {

    private static final Logger logger = Logger.getLogger(IapSecurityAugmentor.class);

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "mock.auth", defaultValue = "false")
    boolean mockAuth;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        // Bypass conditionnel de l'authentification
        if (mockAuth && identity.isAnonymous()) {
            logger.warn("mock.auth activé : identité de développement admin/membre accordée");
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
            String name = jwt.getClaim("name");

            if (email != null && email.endsWith("@zenika.com")) {
                return context.runBlocking(() -> enrichIdentity(identity, email, name));
            } else {
                // Email invalide ou non Zenika : on ne donne pas de rôles
                return Uni.createFrom().item(identity);
            }
        }

        return Uni.createFrom().item(identity);
    }

    private SecurityIdentity enrichIdentity(SecurityIdentity identity, String email, String name) {
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                // Création auto avec rôle par défaut 'membre', nom récupéré depuis le SSO
                user = User.builder().email(email).name(name).role("membre").build();
                userRepository.create(user);
            } else if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
                // Compte existant créé avant l'ajout du champ name : on le complète depuis le SSO
                user.setName(name);
                userRepository.update(email, user);
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
