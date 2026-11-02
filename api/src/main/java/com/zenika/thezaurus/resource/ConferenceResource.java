package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.ConferenceService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/conferences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
public class ConferenceResource {

    @Inject
    ConferenceService service;

    @GET
    public List<Conference> list() throws ExecutionException, InterruptedException {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public RestResponse<Conference> get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        Conference conference = service.findById(id);
        if (conference == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(conference);
    }

    @POST
    public RestResponse<Conference> create(Conference conference) throws ExecutionException, InterruptedException {
        if (isTypeOrReachMissing(conference)) {
            return RestResponse.status(RestResponse.Status.BAD_REQUEST);
        }
        Conference created = service.create(conference);
        return RestResponse.status(RestResponse.Status.CREATED, created);
    }

    @PUT
    @Path("/{id}")
    public RestResponse<Conference> update(@PathParam("id") String id, Conference conference)
            throws ExecutionException, InterruptedException {
        if (isTypeOrReachMissing(conference)) {
            return RestResponse.status(RestResponse.Status.BAD_REQUEST);
        }
        Conference updated = service.update(id, conference);
        if (updated == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(updated);
    }

    private boolean isTypeOrReachMissing(Conference conference) {
        return conference.getType() == null || conference.getReach() == null;
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Role.Names.ADMIN)
    public RestResponse<Void> delete(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        boolean deleted = service.delete(id);
        if (!deleted) {
            return RestResponse.notFound();
        }
        return RestResponse.noContent();
    }
}
