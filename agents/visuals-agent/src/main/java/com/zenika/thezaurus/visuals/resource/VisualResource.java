package com.zenika.thezaurus.visuals.resource;

import com.zenika.thezaurus.visuals.model.VisualGenerationResponse;
import com.zenika.thezaurus.visuals.model.VisualRequest;
import com.zenika.thezaurus.visuals.model.VisualStreamEvent;
import com.zenika.thezaurus.visuals.model.VisualTemplate;
import com.zenika.thezaurus.visuals.model.VisualType;
import com.zenika.thezaurus.visuals.service.TemplateService;
import com.zenika.thezaurus.visuals.service.VisualGenerationService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/visuals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VisualResource {

    @Inject
    TemplateService templateService;

    @Inject
    VisualGenerationService visualGenerationService;

    @GET
    @Path("/templates")
    public List<VisualTemplate> listTemplates(@QueryParam("category") VisualType category) {
        return templateService.listTemplates(category);
    }

    @POST
    @RunOnVirtualThread
    public RestResponse<VisualGenerationResponse> generateVisual(@Valid VisualRequest request) {
        VisualGenerationResponse response = visualGenerationService.generateVisual(request);
        return RestResponse.ok(response);
    }

    @POST
    @Path("/talks")
    @RunOnVirtualThread
    public RestResponse<VisualGenerationResponse> generateTalkVisual(@Valid VisualRequest request) {
        VisualRequest effectiveRequest = request.type() == null ? request.withType("talk") : request;
        VisualGenerationResponse response = visualGenerationService.generateVisual(effectiveRequest);
        return RestResponse.ok(response);
    }

    @POST
    @Path("/blogs")
    @RunOnVirtualThread
    public RestResponse<VisualGenerationResponse> generateBlogVisual(@Valid VisualRequest request) {
        VisualRequest effectiveRequest = request.type() == null ? request.withType("blog") : request;
        VisualGenerationResponse response = visualGenerationService.generateVisual(effectiveRequest);
        return RestResponse.ok(response);
    }

    @POST
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<VisualStreamEvent> streamVisual(@Valid VisualRequest request) {
        return visualGenerationService.streamVisual(request);
    }

    @POST
    @Path("/talks/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<VisualStreamEvent> streamTalkVisual(@Valid VisualRequest request) {
        VisualRequest effectiveRequest = request.type() == null ? request.withType("talk") : request;
        return visualGenerationService.streamVisual(effectiveRequest);
    }

    @POST
    @Path("/blogs/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<VisualStreamEvent> streamBlogVisual(@Valid VisualRequest request) {
        VisualRequest effectiveRequest = request.type() == null ? request.withType("blog") : request;
        return visualGenerationService.streamVisual(effectiveRequest);
    }
}
