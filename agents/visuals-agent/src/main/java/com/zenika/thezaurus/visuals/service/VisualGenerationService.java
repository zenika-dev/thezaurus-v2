package com.zenika.thezaurus.visuals.service;

import com.zenika.thezaurus.visuals.client.ImageInputClient;
import com.zenika.thezaurus.visuals.crop.AutoCropService;
import com.zenika.thezaurus.visuals.model.*;
import com.zenika.thezaurus.visuals.render.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

@ApplicationScoped
public class VisualGenerationService {
    @Inject
    TemplateCatalog catalog;

    @Inject
    TemplateService templates;

    @Inject
    ImageRenderer renderer;

    @Inject
    AutoCropService cropper;

    @Inject
    ImageInputClient images;

    public VisualGenerationResponse generateTalkVisual(TalkVisualRequest request) {
        if (request == null || request.conference() == null)
            throw new IllegalArgumentException("La conférence est requise.");
        String conference = text(request.conference().name());
        String title, title2 = null, id;
        List<PersonInput> people;
        if (request.talks() != null && !request.talks().isEmpty()) {
            if (request.talks().size() != 2
                    || request.speakers() != null && !request.speakers().isEmpty())
                throw new IllegalArgumentException("Deux talks distincts sont requis, sans speakers au niveau racine.");
            var first = request.talks().get(0);
            var second = request.talks().get(1);
            if (first == null || second == null) throw new IllegalArgumentException("Talk invalide.");
            title = text(first.title());
            title2 = text(second.title());
            people = List.of(
                    people(first.speakers(), 1, 1).getFirst(),
                    people(second.speakers(), 1, 1).getFirst());
            id = "conf-2";
        } else {
            title = text(request.title());
            people = people(request.speakers(), 1, 3);
            id = switch (people.size()) {
                case 1 -> "conf-1";
                case 2 -> "conf-1-binome";
                default -> "conf-1-trinome";
            };
        }
        return generate(
                id,
                title,
                title2,
                conference,
                null,
                null,
                null,
                null,
                people,
                request.conference().logoBase64(),
                request.autoCrop());
    }

    public VisualGenerationResponse generateBlogVisual(BlogVisualRequest request) {
        if (request == null) throw new IllegalArgumentException("La requête est requise.");
        var people = people(request.writers(), 1, 2);
        return generate(
                people.size() == 1 ? "blog" : "blog-2",
                text(request.title()),
                null,
                null,
                null,
                null,
                null,
                null,
                people,
                null,
                request.autoCrop());
    }

    public VisualGenerationResponse generateNightClazzVisual(NightClazzVisualRequest request) {
        if (request == null) throw new IllegalArgumentException("La requête est requise.");
        boolean partner = request.partner() != null;
        var people = people(request.speakers(), 1, partner ? 1 : 2);
        return generate(
                partner ? "nc-partenaire" : "nc",
                text(request.title()),
                null,
                null,
                optionalText(request.date()),
                optionalText(request.hour()),
                optionalText(request.location()),
                partner ? text(request.partner().name()) : null,
                people,
                partner ? request.partner().logoBase64() : null,
                request.autoCrop());
    }

    private VisualGenerationResponse generate(
            String id,
            String title,
            String title2,
            String conference,
            String date,
            String hour,
            String location,
            String partner,
            List<PersonInput> inputs,
            String logo,
            Boolean autoCrop) {
        TemplateSpec spec = catalog.require(id);
        List<RenderData.Person> people = new ArrayList<>();
        List<AutoCropService.CropResult> crops = new ArrayList<>();
        AutoCropService.Usage tokens = AutoCropService.Usage.zero();
        int calls = 0, hits = 0;
        // Validate every supplied image before the first paid analysis.
        List<ImageInputClient.Image> photos = inputs.stream()
                .map(person -> person.photo() == null
                        ? null
                        : images.load(
                                person.photo().base64() != null
                                        ? person.photo().base64()
                                        : person.photo().url()))
                .toList();
        var logoImage = images.load(logo);
        for (int i = 0; i < inputs.size(); i++) {
            var photo = photos.get(i);
            RenderData.Crop crop = RenderData.Crop.centered();
            if (photo != null && !Boolean.FALSE.equals(autoCrop)) {
                var slot = spec.photos().get(i);
                var result = cropper.analyze(
                        photo.bytes(),
                        photo.width(),
                        photo.height(),
                        slot.box().get(2).intValue(),
                        slot.box().get(3).intValue(),
                        (int) slot.radius());
                crop = new RenderData.Crop(
                        result.crop().zoom(), result.crop().x(), result.crop().y());
                crops.add(result);
                tokens = tokens.plus(result.usage());
                calls += result.calls();
                if (result.cacheHit()) hits++;
            } else {
                crops.add(new AutoCropService.CropResult(
                        com.zenika.thezaurus.visuals.crop.CropGeometry.Crop.centered(),
                        photo == null ? "placeholder" : "disabled",
                        null,
                        List.of(),
                        AutoCropService.Usage.zero(),
                        0,
                        false));
            }
            people.add(new RenderData.Person(inputs.get(i).name(), photo == null ? null : photo.bytes(), crop));
        }
        var result = renderer.render(new RenderData(
                id,
                title,
                title2,
                conference,
                date,
                hour,
                location,
                partner,
                people,
                logoImage == null ? null : logoImage.bytes()));
        return new VisualGenerationResponse(
                TemplateService.type(id),
                templates.name(id),
                calls > 0 ? cropper.model() : "java2d",
                "image/png",
                Base64.getEncoder().encodeToString(result.png()),
                null,
                Instant.now(),
                result.width(),
                result.height(),
                List.copyOf(crops),
                new VisualGenerationResponse.Usage(
                        calls, tokens.inputTokens(), tokens.outputTokens(), tokens.totalTokens(), hits));
    }

    private static String text(String value) {
        if (value == null || value.isBlank() || value.length() > 500)
            throw new IllegalArgumentException("Texte requis (500 caractères maximum).");
        return value.trim();
    }

    private static String optionalText(String value) {
        if (value != null && value.length() > 500)
            throw new IllegalArgumentException("Texte trop long (500 caractères maximum).");
        return value == null ? null : value.trim();
    }

    private static List<PersonInput> people(List<PersonInput> values, int min, int max) {
        if (values == null || values.size() < min || values.size() > max)
            throw new IllegalArgumentException("Nombre de personnes attendu : " + min + " à " + max + ".");
        for (var person : values) {
            if (person == null) throw new IllegalArgumentException("Personne invalide.");
            text(person.name());
        }
        return List.copyOf(values);
    }

    public Multi<VisualStreamEvent> streamTalkVisual(TalkVisualRequest request) {
        return stream(() -> generateTalkVisual(request));
    }

    public Multi<VisualStreamEvent> streamBlogVisual(BlogVisualRequest request) {
        return stream(() -> generateBlogVisual(request));
    }

    public Multi<VisualStreamEvent> streamNightClazzVisual(NightClazzVisualRequest request) {
        return stream(() -> generateNightClazzVisual(request));
    }

    private Multi<VisualStreamEvent> stream(Supplier<VisualGenerationResponse> generate) {
        return Multi.createFrom()
                .<VisualStreamEvent>emitter(emitter -> {
                    emitter.emit(VisualStreamEvent.of(VisualStreamEvent.Status.STARTED, "Requête reçue."));
                    emitter.emit(VisualStreamEvent.of(
                            VisualStreamEvent.Status.GENERATING, "Analyse du cadrage et rendu des templates."));
                    try {
                        emitter.emit(VisualStreamEvent.completed("Visuel généré.", generate.get()));
                    } catch (RuntimeException exception) {
                        emitter.emit(VisualStreamEvent.of(
                                VisualStreamEvent.Status.FAILED,
                                exception instanceof IllegalArgumentException
                                                || exception instanceof com.zenika.thezaurus.visuals.crop.CropException
                                        ? exception.getMessage()
                                        : "La génération du visuel a échoué."));
                    }
                    emitter.complete();
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
