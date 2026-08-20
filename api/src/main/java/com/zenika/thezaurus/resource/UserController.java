package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.repository.UserRepository;
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

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    SecurityIdentity identity;

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "thezaurus.users.max-results", defaultValue = "500")
    int maxResults;

    @GET
    @Path("/me")
    @RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
    public Response getCurrentUser() {
        if (identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", identity.getPrincipal().getName());
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
