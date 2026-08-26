package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.slack.SlackUserResolver;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class);

    @Inject
    SecurityIdentity identity;

    @Inject
    UserRepository userRepository;

    @Inject
    SlackUserResolver slackUserResolver;

    @ConfigProperty(name = "thezaurus.users.max-results", defaultValue = "500")
    int maxResults;

    /**
     * Appelé une fois par connexion (callback {@code jwt} de NextAuth) : seul point d'accroche
     * « au login », d'où le rattachement Slack ici plutôt que dans l'augmentor, qui tourne à
     * chaque requête.
     */
    @GET
    @Path("/me")
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public Response getCurrentUser() throws ExecutionException, InterruptedException {
        if (identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String email = identity.getPrincipal().getName();

        User user = userRepository.findByEmail(email);
        if (user != null && (user.slackUserId() == null || user.slackUserId().isBlank())) {
            try {
                slackUserResolver.resolveAndPersistAsync(email);
            } catch (Exception e) {
                // Enrichissement, jamais un prérequis : la connexion doit aboutir quoi qu'il arrive.
                logger.warnf(e, "Rattachement Slack non déclenché pour %s", email);
            }
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", email);
        userProfile.put("roles", identity.getRoles());

        return Response.ok(userProfile).build();
    }

    /**
     * Annuaire des utilisateurs persistés, pour alimenter le picker de speakers.
     * Projection volontairement réduite à {name, email} : les rôles ne sortent pas de l'API
     * (l'administration passe par UserAdminResource, réservé aux admins).
     */
    @GET
    @Path("/users")
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public List<UserSummary> listUsers() throws ExecutionException, InterruptedException {
        return userRepository.findAll(maxResults).stream()
                .map(u -> new UserSummary(u.name(), u.email()))
                .toList();
    }

    public record UserSummary(String name, String email) {}
}
