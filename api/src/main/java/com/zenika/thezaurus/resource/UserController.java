package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.slack.SlackUserResolver;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

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

    public record CurrentUserView(String email, Set<String> roles) {}

    /**
     * Appelé une fois par connexion (callback {@code jwt} de NextAuth) : seul point d'accroche
     * « au login », d'où le rattachement Slack ici plutôt que dans l'augmentor, qui tourne à
     * chaque requête.
     */
    @GET
    @Path("/me")
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public RestResponse<CurrentUserView> getCurrentUser() throws ExecutionException, InterruptedException {
        if (identity.isAnonymous()) {
            return RestResponse.status(RestResponse.Status.UNAUTHORIZED);
        }

        String email = identity.getPrincipal().getName();

        User user = userRepository.findByEmail(email);
        if (user != null && (user.slackUserId() == null || user.slackUserId().isBlank())) {
            try {
                slackUserResolver.resolveAndPersistAsync(email);
            } catch (Exception exception) {
                // Enrichissement, jamais un prérequis : la connexion doit aboutir quoi qu'il arrive.
                logger.warnf(exception, "Rattachement Slack non déclenché pour %s", email);
            }
        }

        return RestResponse.ok(new CurrentUserView(email, identity.getRoles()));
    }

    /**
     * Annuaire des utilisateurs persistés, pour alimenter le picker de speakers.
     * Projection volontairement réduite à {name, email} : les rôles ne sortent pas de l'API
     * (l'administration passe par UserAdminResource, réservé aux admins).
     */
    @GET
    @Path("/users")
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public RestResponse<List<UserSummary>> listUsers(@QueryParam("query") @Size(max = 100) String query)
            throws ExecutionException, InterruptedException {
        List<User> users = userRepository.search(query, 20);
        List<UserSummary> result = users.stream()
                .map(currentUser -> new UserSummary(currentUser.name(), currentUser.email()))
                .toList();
        return RestResponse.ok(result);
    }

    public record UserSummary(String name, String email) {}
}
