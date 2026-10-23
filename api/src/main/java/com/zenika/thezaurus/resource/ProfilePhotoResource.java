package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.ProfilePhotoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;

@Path("/api/users/{email}/photo")
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
public class ProfilePhotoResource {
    @Inject
    ProfilePhotoService photos;

    @GET
    @Produces("image/png")
    public Response read(@PathParam("email") String email) throws ExecutionException, InterruptedException {
        var photo = photos.read(photos.requireUser(email));
        return Response.ok(photo.bytes(), photo.contentType())
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}
