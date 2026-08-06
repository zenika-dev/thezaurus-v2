package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    SecurityIdentity identity;

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name = "mock.auth", defaultValue = "false")
    boolean mockAuth;

    @GET
    @Path("/me")
    @RolesAllowed({"membre", "admin"})
    public Response getCurrentUser() {
        if (identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", identity.getPrincipal().getName());
        userProfile.put("roles", identity.getRoles());

        return Response.ok(userProfile).build();
    }

    @GET
    @Path("/users/summary")
    @RolesAllowed({"membre", "admin"})
    public List<UserSummary> listUsers() throws ExecutionException, InterruptedException {
        return userRepository.findAll().stream()
                .map(u -> new UserSummary(u.getName(), u.getEmail()))
                .toList();
    }

    public record UserSummary(String name, String email) {}
}
