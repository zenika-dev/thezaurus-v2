package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.service.ConferenceService;
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
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Path("/conferences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConferenceResource {

    @Inject
    ConferenceService service;

    @GET
    public List<Conference> list() throws ExecutionException, InterruptedException {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        Conference conference = service.findById(id);
        if (conference == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(conference).build();
    }

    @POST
    public Response create(Conference conference) throws ExecutionException, InterruptedException {
        Conference created = service.create(conference);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, Conference conference) throws ExecutionException, InterruptedException {
        Conference updated = service.update(id, conference);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        boolean deleted = service.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
