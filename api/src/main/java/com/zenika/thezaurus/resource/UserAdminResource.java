package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Administration des utilisateurs, réservée aux admins. Les rôles étant exclus de la
 * sérialisation JSON de {@link User} (protection de l'API générale), ce resource expose sa
 * propre projection {@link UserAdminView} qui les inclut. Le chemin est distinct de
 * GET /api/users (annuaire du picker de speakers, accessible à tous les rôles).
 */
@Path("/api/admin/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(Role.Names.ADMIN)
public class UserAdminResource {

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "thezaurus.users.max-results", defaultValue = "500")
    int maxResults;

    public record UserAdminView(String name, String email, List<Role> roles) {
        static UserAdminView of(User user) {
            return new UserAdminView(user.name(), user.email(),
                    user.roles() == null ? List.of() : user.roles());
        }
    }

    @GET
    public List<UserAdminView> list() throws ExecutionException, InterruptedException {
        return userRepository.findAll(maxResults).stream()
                .map(UserAdminView::of)
                .toList();
    }

    public static class RolesUpdateRequest {
        public List<String> roles;
    }

    @PUT
    @Path("/{email}/roles")
    public Response updateRoles(@PathParam("email") String email, RolesUpdateRequest request)
            throws ExecutionException, InterruptedException {
        // Les documents users sont keyés par l'email en minuscules (cf. IapSecurityAugmentor) :
        // on normalise le path param pour retrouver le document quelle que soit la casse saisie.
        email = email.toLowerCase(Locale.ROOT);
        if (request == null || request.roles == null || request.roles.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "La liste des rôles est obligatoire"))
                    .build();
        }

        List<String> invalid = request.roles.stream().filter(r -> !Role.isValid(r)).toList();
        if (!invalid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Rôles inconnus : " + invalid + ", rôles autorisés : " + Role.ALL))
                    .build();
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        user = user.withRoles(request.roles.stream().map(Role::valueOf).distinct().toList());
        userRepository.update(email, user);
        return Response.ok(UserAdminView.of(user)).build();
    }
}
