package com.zenika.thezaurus.resource;

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
import java.util.Map;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    SecurityIdentity identity;

    @ConfigProperty(name = "mock.auth", defaultValue = "false")
    boolean mockAuth;

    @GET
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
}
