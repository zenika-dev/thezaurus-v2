package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Event;
import com.zenika.thezaurus.model.EventsDashboard;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.EventService;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Year;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
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
    public RestResponse<Event> get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        Event event = service.findById(id);
        if (event == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(event);
    }

    @POST
    public RestResponse<Event> create(Event event) throws ExecutionException, InterruptedException {
        Event created = service.create(event);
        return RestResponse.status(RestResponse.Status.CREATED, created);
    }

    @PUT
    @Path("/{id}")
    public RestResponse<Event> update(@PathParam("id") String id, Event event)
            throws ExecutionException, InterruptedException {
        Event updated = service.update(id, event);
        if (updated == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(updated);
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
