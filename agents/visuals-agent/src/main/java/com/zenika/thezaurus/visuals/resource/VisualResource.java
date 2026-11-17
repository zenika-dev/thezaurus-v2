package com.zenika.thezaurus.visuals.resource;

import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.NightClazzVisualRequest;
import com.zenika.thezaurus.visuals.model.TalkVisualRequest;
import com.zenika.thezaurus.visuals.model.VisualGenerationResponse;
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
import java.util.Base64;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
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
    @Path("/talks")
    @RunOnVirtualThread
    public RestResponse<VisualGenerationResponse> generateTalkVisual(@Valid TalkVisualRequest request) {
        VisualGenerationResponse response = visualGenerationService.generateTalkVisual(request);
        return RestResponse.ok(response);
    }

    @POST
    @Path("/blogs")
    @RunOnVirtualThread
    public RestResponse<VisualGenerationResponse> generateBlogVisual(@Valid BlogVisualRequest request) {
        VisualGenerationResponse response = visualGenerationService.generateBlogVisual(request);
        return RestResponse.ok(response);
    }

    @POST
    @Path("/nightclazz")
    @RunOnVirtualThread
    public RestResponse<VisualGenerationResponse> generateNightClazzVisual(@Valid NightClazzVisualRequest request) {
        VisualGenerationResponse response = visualGenerationService.generateNightClazzVisual(request);
        return RestResponse.ok(response);
    }

    @POST
    @Path("/talks/image")
    @Produces("image/png")
    @RunOnVirtualThread
    public RestResponse<byte[]> generateTalkVisualImage(@Valid TalkVisualRequest request) {
        return toBinaryImageResponse(visualGenerationService.generateTalkVisual(request));
    }

    @POST
    @Path("/blogs/image")
    @Produces("image/png")
    @RunOnVirtualThread
    public RestResponse<byte[]> generateBlogVisualImage(@Valid BlogVisualRequest request) {
        return toBinaryImageResponse(visualGenerationService.generateBlogVisual(request));
    }

    @POST
    @Path("/nightclazz/image")
    @Produces("image/png")
    @RunOnVirtualThread
    public RestResponse<byte[]> generateNightClazzVisualImage(@Valid NightClazzVisualRequest request) {
        return toBinaryImageResponse(visualGenerationService.generateNightClazzVisual(request));
    }

    private RestResponse<byte[]> toBinaryImageResponse(VisualGenerationResponse response) {
        byte[] imageBytes = Base64.getDecoder().decode(response.imageBase64());
        String mimeType = response.mimeType() != null ? response.mimeType() : "image/png";
        return ResponseBuilder.ok(imageBytes, mimeType)
                .header("X-Visual-Template", response.templateName())
                .build();
    }

    @POST
    @Path("/talks/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<VisualStreamEvent> streamTalkVisual(@Valid TalkVisualRequest request) {
        return visualGenerationService.streamTalkVisual(request);
    }

    @POST
    @Path("/blogs/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<VisualStreamEvent> streamBlogVisual(@Valid BlogVisualRequest request) {
        return visualGenerationService.streamBlogVisual(request);
    }

    @POST
    @Path("/nightclazz/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<VisualStreamEvent> streamNightClazzVisual(@Valid NightClazzVisualRequest request) {
        return visualGenerationService.streamNightClazzVisual(request);
    }
}
