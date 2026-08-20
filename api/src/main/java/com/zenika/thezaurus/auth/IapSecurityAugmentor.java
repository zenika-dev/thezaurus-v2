package com.zenika.thezaurus.auth;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

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
            logger.warn("mock.auth activé : identité de développement admin/consultant accordée");
            SecurityIdentity devIdentity = QuarkusSecurityIdentity.builder(identity)
                    .setPrincipal(() -> "dev@zenika.com")
                    .addRole(Role.Names.ADMIN)
                    .addRole(Role.Names.CONSULTANT)
                    .build();
            return Uni.createFrom().item(devIdentity);
        }

        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        // Si l'identité provient du JWT (IAP)
        if (identity.getPrincipal() instanceof JsonWebToken) {
            JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
            String rawEmail = jwt.getClaim("email");
            String name = jwt.getClaim("name");

            // Normalisé en minuscules : l'email sert de clé de document Firestore et de
            // principal, une casse différente ne doit ni refuser l'accès ni créer un doublon.
            String email = rawEmail == null ? null : rawEmail.toLowerCase(Locale.ROOT);

            if (email != null && email.endsWith("@zenika.com")) {
                return context.runBlocking(() -> enrichIdentity(identity, email, name));
            } else {
                // Email invalide ou non Zenika : on ne donne pas de rôles
                logger.warnf("Email absent ou hors domaine zenika.com (%s), identité non enrichie", rawEmail);
                return Uni.createFrom().item(identity);
            }
        }

        return Uni.createFrom().item(identity);
    }

    private SecurityIdentity enrichIdentity(SecurityIdentity identity, String email, String name) {
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                // Création auto avec rôle par défaut 'consultant', nom récupéré depuis le SSO
                user = User.builder()
                        .email(email)
                        .name(name)
                        .roles(List.of(Role.CONSULTANT))
                        .build();
                userRepository.create(user);
                logger.infof("Utilisateur %s créé automatiquement avec le rôle '%s'", email, Role.CONSULTANT);
            } else if ((user.name() == null || user.name().isBlank()) && name != null && !name.isBlank()) {
                // Compte existant créé avant l'ajout du champ name : on le complète depuis le SSO
                user = user.withName(name);
                userRepository.update(email, user);
            }

            QuarkusSecurityIdentity.Builder builder =
                    QuarkusSecurityIdentity.builder(identity).setPrincipal(() -> email);
            if (user.roles() != null) {
                user.roles().stream().map(Role::name).forEach(builder::addRole);
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Erreur d'accès à Firestore pour l'utilisateur", e);
        }
    }
}
