package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkFormat;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import com.zenika.thezaurus.service.TalkReviewService;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/talks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.Names.ADMIN, Role.Names.DT, Role.Names.CONSULTANT})
public class TalkResource {

    @Inject
    TalkService service;

    @Inject
    TalkReviewService talkReviewService;

    @Inject
    SecurityIdentity identity;

    @GET
    public List<Talk> list() throws ExecutionException, InterruptedException {
        return service.findAll();
    }

    @GET
    @Path("/{id}")
    public RestResponse<Talk> get(@PathParam("id") String id) throws ExecutionException, InterruptedException {
        Talk talk = service.findById(id);
        if (talk == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(talk);
    }

    @POST
    public RestResponse<Talk> create(Talk talk) throws ExecutionException, InterruptedException {
        Talk created = service.create(talk);
        return RestResponse.status(RestResponse.Status.CREATED, created);
    }

    @PUT
    @Path("/{id}")
    public RestResponse<Talk> update(@PathParam("id") String id, @Valid Talk talk)
            throws ExecutionException, InterruptedException {
        Talk existing = service.findById(id);
        if (existing == null) {
            return RestResponse.notFound();
        }
        boolean privileged = identity.hasRole(Role.Names.ADMIN) || identity.hasRole(Role.Names.DT);
        if (!existing.canBeEditedBy(identity.getPrincipal().getName(), privileged)) {
            return RestResponse.status(RestResponse.Status.FORBIDDEN);
        }
        if (talk == null
                || (!TalkFormat.isSupported(talk.format())
                        && !java.util.Objects.equals(talk.format(), existing.format()))) {
            return RestResponse.status(RestResponse.Status.BAD_REQUEST);
        }
        Talk updated = service.update(id, talk);
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

    @POST
    @Path("/review")
    public RestResponse<TalkReviewResponse> review(TalkReviewRequest request) {
        if (request == null || request.title() == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Le titre et l'abstract sont requis"))
                    .build());
        }

        TalkReviewResponse response = talkReviewService.reviewTalk(request);
        return RestResponse.ok(response);
    }
}
