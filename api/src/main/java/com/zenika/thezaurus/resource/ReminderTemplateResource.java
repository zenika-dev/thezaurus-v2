package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.*;
import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.service.ReminderTemplateRenderer;
import com.zenika.thezaurus.service.TalkService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;

@Path("/api/admin/reminder-template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(Role.Names.ADMIN)
public class ReminderTemplateResource {
    @Inject
    ReminderTemplateRepository repository;

    @Inject
    ReminderTemplateRenderer renderer;

    @Inject
    TalkService talks;

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
            throw ReminderTemplateRenderer.invalid("Une révision valide est requise.");
        }
        ReminderTemplateView clean = renderer.validate(request.subject(), request.bodyHtml());
        return repository.save(clean.subject(), clean.bodyHtml(), request.revision());
    }

    @POST
    @Path("/preview")
    public ReminderTemplatePreview preview(ReminderTemplatePreviewRequest request)
            throws ExecutionException, InterruptedException {
        if (request == null
                || request.talkId() == null
                || request.talkId().isBlank()
                || request.talkId().contains("/")) {
            throw ReminderTemplateRenderer.invalid("Sélectionnez un talk pour l’aperçu.");
        }
        Talk talk = talks.findById(request.talkId());
        if (talk == null) throw new ThezaurusException("Talk introuvable.", Response.Status.NOT_FOUND);
        return renderer.render(request.subject(), request.bodyHtml(), talk);
    }
}
