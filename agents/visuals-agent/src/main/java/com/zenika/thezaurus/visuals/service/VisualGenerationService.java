package com.zenika.thezaurus.visuals.service;

import com.zenika.thezaurus.visuals.client.GenAiVisualClient;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.GeneratedImage;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.NightClazzVisualRequest;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.TalkItemInput;
import com.zenika.thezaurus.visuals.model.TalkVisualRequest;
import com.zenika.thezaurus.visuals.model.VisualGenerationResponse;
import com.zenika.thezaurus.visuals.model.VisualStreamEvent;
import com.zenika.thezaurus.visuals.model.VisualStreamEvent.Status;
import com.zenika.thezaurus.visuals.model.VisualTemplate;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VisualGenerationService {

    private static final Logger LOG = Logger.getLogger(VisualGenerationService.class);

    private final TemplateService templateService;
    private final GenAiVisualClient genAiVisualClient;
    private final String geminiModel;
    private final long heartbeatIntervalMs;

    @Inject
    public VisualGenerationService(
            TemplateService templateService,
            GenAiVisualClient genAiVisualClient,
            @ConfigProperty(name = "visuals.gemini.model", defaultValue = "gemini-2.5-flash-image") String geminiModel,
            @ConfigProperty(name = "visuals.sse.heartbeat-interval-ms", defaultValue = "3000")
                    long heartbeatIntervalMs) {
        this.templateService = templateService;
        this.genAiVisualClient = genAiVisualClient;
        this.geminiModel = geminiModel;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public VisualGenerationResponse generateTalkVisual(TalkVisualRequest request) {
        validateTalkRequest(request);

        VisualTemplate template = templateService.resolveTalkTemplate(request);
        String prompt = templateService.buildTalkPrompt(template, request);

        List<InlineMediaPart> mediaParts = new ArrayList<>();
        mediaParts.add(templateService.loadTemplateImage(template));

        if (request.talks() != null && !request.talks().isEmpty()) {
            for (TalkItemInput talk : request.talks()) {
                if (talk != null && talk.speaker() != null) {
                    decodeOptionalBase64Image(talk.speaker().photoBase64()).ifPresent(mediaParts::add);
                }
            }
        } else {
            List<PersonInput> speakers = request.speakers() != null ? request.speakers() : List.of();
            for (PersonInput speaker : speakers) {
                if (speaker != null) {
                    decodeOptionalBase64Image(speaker.photoBase64()).ifPresent(mediaParts::add);
                }
            }
        }
        if (request.conference() != null) {
            decodeOptionalBase64Image(request.conference().logoBase64()).ifPresent(mediaParts::add);
        }

        return executeGeneration(template, prompt, mediaParts);
    }

    public VisualGenerationResponse generateBlogVisual(BlogVisualRequest request) {
        validateBlogRequest(request);

        VisualTemplate template = templateService.resolveBlogTemplate(request);
        String prompt = templateService.buildBlogPrompt(template, request);

        List<InlineMediaPart> mediaParts = new ArrayList<>();
        mediaParts.add(templateService.loadTemplateImage(template));

        List<PersonInput> writers = request.writers() != null ? request.writers() : List.of();
        for (PersonInput writer : writers) {
            if (writer != null) {
                decodeOptionalBase64Image(writer.photoBase64()).ifPresent(mediaParts::add);
            }
        }

        return executeGeneration(template, prompt, mediaParts);
    }

    public VisualGenerationResponse generateNightClazzVisual(NightClazzVisualRequest request) {
        validateNightClazzRequest(request);

        VisualTemplate template = templateService.resolveNightClazzTemplate(request);
        String prompt = templateService.buildNightClazzPrompt(template, request);

        List<InlineMediaPart> mediaParts = new ArrayList<>();
        mediaParts.add(templateService.loadTemplateImage(template));

        List<PersonInput> speakers = request.speakers() != null ? request.speakers() : List.of();
        for (PersonInput speaker : speakers) {
            if (speaker != null) {
                decodeOptionalBase64Image(speaker.photoBase64()).ifPresent(mediaParts::add);
            }
        }
        if (request.partner() != null) {
            decodeOptionalBase64Image(request.partner().logoBase64()).ifPresent(mediaParts::add);
        }

        return executeGeneration(template, prompt, mediaParts);
    }

    public Multi<VisualStreamEvent> streamTalkVisual(TalkVisualRequest request) {
        validateTalkRequest(request);
        VisualTemplate template = templateService.resolveTalkTemplate(request);
        return streamVisualGeneration(
                template, () -> generateTalkVisual(request), "Génération du visuel de talk en cours...");
    }

    public Multi<VisualStreamEvent> streamBlogVisual(BlogVisualRequest request) {
        validateBlogRequest(request);
        VisualTemplate template = templateService.resolveBlogTemplate(request);
        return streamVisualGeneration(
                template, () -> generateBlogVisual(request), "Génération du visuel de blog en cours...");
    }

    public Multi<VisualStreamEvent> streamNightClazzVisual(NightClazzVisualRequest request) {
        validateNightClazzRequest(request);
        VisualTemplate template = templateService.resolveNightClazzTemplate(request);
        return streamVisualGeneration(
                template, () -> generateNightClazzVisual(request), "Génération du visuel de NightClazz en cours...");
    }

    private VisualGenerationResponse executeGeneration(
            VisualTemplate template, String prompt, List<InlineMediaPart> mediaParts) {
        GeneratedImage generated = genAiVisualClient.generateImage(geminiModel, prompt, mediaParts);
        String encoded = Base64.getEncoder().encodeToString(generated.data());

        return new VisualGenerationResponse(
                template.category(),
                template.name(),
                geminiModel,
                generated.mimeType(),
                encoded,
                prompt,
                Instant.now());
    }

    private Multi<VisualStreamEvent> streamVisualGeneration(
            VisualTemplate template, Supplier<VisualGenerationResponse> generationSupplier, String progressMessage) {

        Multi<VisualStreamEvent> initialEvents = Multi.createFrom()
                .items(
                        VisualStreamEvent.of(
                                Status.STARTED, "Initialisation de la génération avec le modèle " + geminiModel),
                        VisualStreamEvent.of(Status.TEMPLATE_LOADED, "Template sélectionné : " + template.name()),
                        VisualStreamEvent.of(Status.GENERATING, progressMessage));

        Uni<VisualStreamEvent> completionUni = Uni.createFrom()
                .item(generationSupplier)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .map(res -> VisualStreamEvent.completed("Visuel généré avec succès", res))
                .onFailure()
                .recoverWithItem(err -> {
                    LOG.warn("Échec lors du flux SSE de génération de visuel", err);
                    return VisualStreamEvent.of(Status.FAILED, err.getMessage());
                });

        return Multi.createBy()
                .concatenating()
                .streams(initialEvents, Multi.createFrom().emitter(emitter -> {
                    var heartbeatCancellable = Multi.createFrom()
                            .ticks()
                            .every(Duration.ofMillis(Math.max(50L, heartbeatIntervalMs)))
                            .subscribe()
                            .with(tick -> emitter.emit(VisualStreamEvent.of(
                                    Status.HEARTBEAT, "Génération IA toujours en cours (" + geminiModel + ")...")));

                    completionUni.subscribe().with(finalEvent -> {
                        heartbeatCancellable.cancel();
                        emitter.emit(finalEvent);
                        emitter.complete();
                    });
                }));
    }

    private Optional<InlineMediaPart> decodeOptionalBase64Image(String base64Input) {
        if (base64Input == null || base64Input.isBlank()) {
            return Optional.empty();
        }
        String raw = base64Input.trim();
        String mimeType = "image/png";
        if (raw.startsWith("data:")) {
            int commaIdx = raw.indexOf(',');
            if (commaIdx > 0) {
                String header = raw.substring(5, commaIdx);
                int semiIdx = header.indexOf(';');
                if (semiIdx > 0) {
                    mimeType = header.substring(0, semiIdx);
                }
                raw = raw.substring(commaIdx + 1);
            }
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(raw);
            return Optional.of(new InlineMediaPart(decoded, mimeType));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Image Base64 invalide fournie dans la requête", e);
        }
    }

    private void validateTalkRequest(TalkVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de génération de visuel de talk est requise.");
        }
        if (!request.isTitleValid()) {
            throw new IllegalArgumentException("Le titre du talk est requis.");
        }
    }

    private void validateBlogRequest(BlogVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de génération de visuel de blog est requise.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Le titre du blog post est requis.");
        }
    }

    private void validateNightClazzRequest(NightClazzVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de génération de visuel NightClazz est requise.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Le titre du NightClazz est requis.");
        }
    }
}
