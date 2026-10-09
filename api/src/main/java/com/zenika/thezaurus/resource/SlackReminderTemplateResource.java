package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.*;
import com.zenika.thezaurus.repository.SlackReminderTemplateRepository;
import com.zenika.thezaurus.service.SlackReminderTemplateRenderer;
import com.zenika.thezaurus.service.TalkService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;

@Path("/api/admin/slack-reminder-template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(Role.Names.ADMIN)
public class SlackReminderTemplateResource {
    @Inject
    SlackReminderTemplateRepository repository;

    @Inject
    SlackReminderTemplateRenderer renderer;

    @Inject
    TalkService talks;

    @GET
    @Path("/contexts")
    public TemplateContextPage contexts(@QueryParam("cursor") String cursor)
            throws ExecutionException, InterruptedException {
        if (cursor != null && (cursor.isBlank() || cursor.contains("/") || cursor.length() > 1500)) {
            throw SlackReminderTemplateRenderer.invalid("Curseur de pagination invalide.");
        }
        return talks.findContextOptions(cursor);
    }

    @GET
    public ReminderTemplateView get() throws ExecutionException, InterruptedException {
        return repository.get();
    }

    @PUT
    public ReminderTemplateView save(ReminderTemplateUpdate request) throws ExecutionException, InterruptedException {
        if (request == null
                || request.revision() == null
                || request.revision() < 0
                || request.revision() == Long.MAX_VALUE) {
            throw SlackReminderTemplateRenderer.invalid("Une révision valide est requise.");
        }
        ReminderTemplateView clean = renderer.validate(request.bodyHtml());
        return repository.save(clean.bodyHtml(), request.revision());
    }

    @POST
    @Path("/preview")
    public ReminderTemplatePreview preview(ReminderTemplatePreviewRequest request)
            throws ExecutionException, InterruptedException {
        if (request == null
                || request.contextId() == null
                || request.contextId().isBlank()
                || request.contextId().contains("/")) {
            throw SlackReminderTemplateRenderer.invalid("Sélectionnez un talk pour l’aperçu.");
        }
        Talk talk = talks.findById(request.contextId());
        if (talk == null) throw new ThezaurusException("Talk introuvable.", Response.Status.NOT_FOUND);
        return renderer.renderPreview(request.bodyHtml(), talk);
    }
}
