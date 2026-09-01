package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import com.zenika.thezaurus.service.TalkReviewService;
import com.zenika.thezaurus.service.TalkService;
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
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Path("/talks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
public class TalkResource {

    @Inject
    TalkService service;

    @Inject
    TalkReviewService talkReviewService;

    @GET
    public List<Talk> list() throws ExecutionException, InterruptedException {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        Talk talk = service.findById(id);
        if (talk == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(talk).build();
    }

    @POST
    public Response create(Talk talk) throws ExecutionException, InterruptedException {
        Talk created = service.create(talk);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, Talk talk) throws ExecutionException, InterruptedException {
        Talk updated = service.update(id, talk);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Role.Names.ADMIN)
    public Response delete(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        boolean deleted = service.delete(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/review")
    public Response review(TalkReviewRequest request) {
        if (request == null || request.title() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error","Le titre et l'abstract sont requis"))
                    .build();
        }

        TalkReviewResponse response = talkReviewService.reviewTalk(request);
        return Response.ok(response).build();
    }
}
