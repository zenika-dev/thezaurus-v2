package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Event;
import com.zenika.thezaurus.model.EventsDashboard;
import com.zenika.thezaurus.service.EventService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Year;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    EventService service;

    @GET
    @Path("/dashboard")
    public EventsDashboard dashboard(@QueryParam("year") Integer year) throws ExecutionException, InterruptedException {
        int resolvedYear = year != null ? year : Year.now().getValue();
        return service.getDashboard(resolvedYear);
    }

    @GET
    public List<Event> list(@QueryParam("page") Integer page, @QueryParam("size") Integer size)
            throws ExecutionException, InterruptedException {
        if (size == null) {
            return service.findAll();
        }
        return service.findAll(page, size);
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        Event event = service.findById(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(event).build();
    }

    @POST
    public Response create(Event event) throws ExecutionException, InterruptedException {
        Event created = service.create(event);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, Event event) throws ExecutionException, InterruptedException {
        Event updated = service.update(id, event);
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
