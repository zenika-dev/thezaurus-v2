package com.zenika.thezaurus.visuals.service;

import com.zenika.thezaurus.visuals.client.GenAiVisualClient;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.GeneratedImage;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.NightClazzVisualRequest;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.PhotoInput;
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VisualGenerationService {

    private static final Logger LOG = Logger.getLogger(VisualGenerationService.class);
    private static final byte[] DEFAULT_PERSON_ICON_PNG = createDefaultPersonIconPng();

    private final TemplateService templateService;
    private final GenAiVisualClient genAiVisualClient;
    private final String geminiModel;
    private final long heartbeatIntervalMs;
    private final HttpClient httpClient;

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
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public VisualGenerationResponse generateTalkVisual(TalkVisualRequest request) {
        validateTalkRequest(request);

        VisualTemplate template = templateService.resolveTalkTemplate(request);
        String prompt = templateService.buildTalkPrompt(template, request);

        List<InlineMediaPart> mediaParts = new ArrayList<>();
        mediaParts.add(templateService.loadTemplateImage(template));

        List<PersonInput> speakers = request.speakers() != null ? request.speakers() : List.of();
        for (PersonInput speaker : speakers) {
            if (speaker != null) {
                mediaParts.add(resolvePersonPhotoOrDefaultIcon(speaker.photo()));
            }
        }
        if (request.conference() != null) {
            resolveOptionalImage(request.conference().logoBase64()).ifPresent(mediaParts::add);
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
                mediaParts.add(resolvePersonPhotoOrDefaultIcon(writer.photo()));
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
                mediaParts.add(resolvePersonPhotoOrDefaultIcon(speaker.photo()));
            }
        }
        if (request.partner() != null) {
            resolveOptionalImage(request.partner().logoBase64()).ifPresent(mediaParts::add);
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

    InlineMediaPart resolvePersonPhotoOrDefaultIcon(PhotoInput photo) {
        if (photo != null) {
            Optional<InlineMediaPart> fromBase64 = decodeOptionalBase64Image(photo.base64());
            if (fromBase64.isPresent()) {
                return fromBase64.get();
            }
            Optional<InlineMediaPart> fromUrl = fetchImageFromUrl(photo.url());
            if (fromUrl.isPresent()) {
                return fromUrl.get();
            }
        }
        return defaultPersonIconPart();
    }

    Optional<InlineMediaPart> resolveOptionalImage(String input) {
        if (!isMeaningfulValue(input)) {
            return Optional.empty();
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return fetchImageFromUrl(trimmed);
        }
        return decodeOptionalBase64Image(trimmed);
    }

    Optional<InlineMediaPart> decodeOptionalBase64Image(String base64Input) {
        if (!isMeaningfulValue(base64Input)) {
            return Optional.empty();
        }
        String raw = base64Input.trim();
        if (raw.startsWith("data:")) {
            int commaIdx = raw.indexOf(',');
            if (commaIdx > 0) {
                raw = raw.substring(commaIdx + 1);
            }
        }
        try {
            byte[] decoded = Base64.getMimeDecoder().decode(raw);
            Optional<String> detectedMime = detectImageMimeType(decoded);
            if (detectedMime.isEmpty()) {
                LOG.warn(
                        "Les données Base64 fournies ne correspondent pas à un format d'image valide (PNG/JPEG/GIF/WEBP) ; ignorées.");
                return Optional.empty();
            }
            return Optional.of(new InlineMediaPart(decoded, detectedMime.get()));
        } catch (IllegalArgumentException e) {
            LOG.warn("Chaîne Base64 invalide fournie dans la requête ; ignorée.", e);
            return Optional.empty();
        }
    }

    Optional<InlineMediaPart> fetchImageFromUrl(String urlInput) {
        if (!isMeaningfulValue(urlInput)) {
            return Optional.empty();
        }
        String trimmed = urlInput.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            LOG.warnf("URL d'image ignorée car le schéma n'est pas HTTP(S) : %s", trimmed);
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimmed))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "TheZaurus-VisualsAgent/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warnf(
                        "Échec du téléchargement de l'image depuis %s (statut HTTP %d)",
                        trimmed, response.statusCode());
                return Optional.empty();
            }
            byte[] body = response.body();
            Optional<String> detectedMime = detectImageMimeType(body);
            if (detectedMime.isEmpty()) {
                LOG.warnf("Le contenu téléchargé depuis %s n'est pas une image valide (PNG/JPEG/GIF/WEBP)", trimmed);
                return Optional.empty();
            }
            return Optional.of(new InlineMediaPart(body, detectedMime.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warnf(e, "Téléchargement de l'image interrompu depuis %s", trimmed);
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf(e, "Impossible de télécharger l'image depuis %s", trimmed);
            return Optional.empty();
        }
    }

    public static InlineMediaPart defaultPersonIconPart() {
        return new InlineMediaPart(DEFAULT_PERSON_ICON_PNG, "image/png");
    }

    public static byte[] getDefaultPersonIconPng() {
        return DEFAULT_PERSON_ICON_PNG.clone();
    }

    static Optional<String> detectImageMimeType(byte[] data) {
        if (data == null || data.length < 4) {
            return Optional.empty();
        }
        if (data.length >= 8
                && (data[0] & 0xFF) == 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47
                && data[4] == 0x0D
                && data[5] == 0x0A
                && data[6] == 0x1A
                && data[7] == 0x0A) {
            return Optional.of("image/png");
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (data.length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8') {
            return Optional.of("image/gif");
        }
        if (data.length >= 12
                && data[0] == 'R'
                && data[1] == 'I'
                && data[2] == 'F'
                && data[3] == 'F'
                && data[8] == 'W'
                && data[9] == 'E'
                && data[10] == 'B'
                && data[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean isMeaningfulValue(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !"string".equalsIgnoreCase(trimmed);
    }

    private static byte[] createDefaultPersonIconPng() {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fond neutre clair
            g2d.setColor(new Color(243, 244, 246));
            g2d.fillRect(0, 0, size, size);

            // Silhouette représentant une personne (tête + épaules)
            g2d.setColor(new Color(55, 65, 81));
            int headDiameter = 160;
            g2d.fillOval((size - headDiameter) / 2, 96, headDiameter, headDiameter);

            int shouldersWidth = 320;
            int shouldersHeight = 260;
            g2d.fillOval((size - shouldersWidth) / 2, 290, shouldersWidth, shouldersHeight);
        } finally {
            g2d.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de générer l'icône de personne par défaut", e);
        }
    }

    private void validateTalkRequest(TalkVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de génération de visuel de talk est requise.");
        }
        if (request.title() == null || request.title().isBlank()) {
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
