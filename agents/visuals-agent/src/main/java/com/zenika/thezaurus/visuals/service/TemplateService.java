package com.zenika.thezaurus.visuals.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.NightClazzVisualRequest;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.TalkItemInput;
import com.zenika.thezaurus.visuals.model.TalkVisualRequest;
import com.zenika.thezaurus.visuals.model.VisualTemplate;
import com.zenika.thezaurus.visuals.model.VisualType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TemplateService {

    private static final Logger LOG = Logger.getLogger(TemplateService.class);

    public static final String DEFAULT_TALK_TEMPLATE = "generate-image-speaker-event";
    public static final String DEFAULT_TALK_DUO_TEMPLATE = "generate-image-2-speaker-event";
    public static final String DEFAULT_TALK_TRIO_TEMPLATE = "generate-image-3-speaker-event";
    public static final String DEFAULT_TALK_TWO_TALKS_TEMPLATE = "generate-image-2-talks-event";
    public static final String DEFAULT_BLOG_TEMPLATE = "generate-image-blog-post";
    public static final String DEFAULT_BLOG_DUO_TEMPLATE = "generate-image-2-blog-post";
    public static final String DEFAULT_NIGHTCLAZZ_TEMPLATE = "generate-image-nightclazz";
    public static final String DEFAULT_NIGHTCLAZZ_PARTNER_TEMPLATE = "generate-image-nightclazz-partner";

    private final List<VisualTemplate> templates;
    private final Optional<String> customTemplatesDir;

    @Inject
    public TemplateService(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "visuals.templates.dir") Optional<String> customTemplatesDir) {
        this.customTemplatesDir = customTemplatesDir;
        this.templates = loadTemplates(objectMapper);
    }

    private List<VisualTemplate> loadTemplates(ObjectMapper objectMapper) {
        try (InputStream is = getClass().getResourceAsStream("/templates.json")) {
            if (is == null) {
                throw new IllegalStateException("Fichier templates.json introuvable dans le classpath");
            }
            List<VisualTemplate> loaded = objectMapper.readValue(is, new TypeReference<List<VisualTemplate>>() {});
            LOG.infof("%d templates de visuels chargés depuis templates.json", loaded.size());
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire ou parser templates.json", e);
        }
    }

    public List<VisualTemplate> listTemplates(VisualType category) {
        if (category == null) {
            return Collections.unmodifiableList(templates);
        }
        return templates.stream().filter(t -> t.category() == category).toList();
    }

    public Optional<VisualTemplate> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim();
        return templates.stream()
                .filter(t -> t.name().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public VisualTemplate resolveTalkTemplate(TalkVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de visuel de talk ne peut pas être nulle.");
        }
        if (request.conference() == null || !isNotBlank(request.conference().name())) {
            throw new IllegalArgumentException("Le nom de la conférence est requis pour un visuel de talk.");
        }

        List<TalkItemInput> talks = sanitizeTalks(request.talks());
        if (!talks.isEmpty()) {
            return switch (talks.size()) {
                case 1 ->
                    findByName(DEFAULT_TALK_TEMPLATE)
                            .orElseThrow(
                                    () -> new IllegalStateException("Template introuvable: " + DEFAULT_TALK_TEMPLATE));
                case 2 ->
                    findByName(DEFAULT_TALK_TWO_TALKS_TEMPLATE)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Template introuvable: " + DEFAULT_TALK_TWO_TALKS_TEMPLATE));
                default ->
                    throw new IllegalArgumentException(
                            "Le nombre de talks pour une conférence doit être compris entre 1 et 2 (reçu : "
                                    + talks.size() + ").");
            };
        }

        List<PersonInput> speakers = sanitizeList(request.speakers());
        if (speakers.isEmpty()) {
            throw new IllegalArgumentException("Au moins un speaker est requis pour un visuel de talk.");
        }

        return switch (speakers.size()) {
            case 1 ->
                findByName(DEFAULT_TALK_TEMPLATE)
                        .orElseThrow(() -> new IllegalStateException("Template introuvable: " + DEFAULT_TALK_TEMPLATE));
            case 2 ->
                findByName(DEFAULT_TALK_DUO_TEMPLATE)
                        .orElseThrow(
                                () -> new IllegalStateException("Template introuvable: " + DEFAULT_TALK_DUO_TEMPLATE));
            case 3 ->
                findByName(DEFAULT_TALK_TRIO_TEMPLATE)
                        .orElseThrow(
                                () -> new IllegalStateException("Template introuvable: " + DEFAULT_TALK_TRIO_TEMPLATE));
            default ->
                throw new IllegalArgumentException(
                        "Le nombre de speakers pour un talk doit être compris entre 1 et 3 (reçu : " + speakers.size()
                                + ").");
        };
    }

    public VisualTemplate resolveBlogTemplate(BlogVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de visuel de blog ne peut pas être nulle.");
        }
        List<PersonInput> writers = sanitizeList(request.writers());
        if (writers.isEmpty()) {
            throw new IllegalArgumentException(
                    "Au moins un auteur (writer) est requis pour un visuel d'article de blog.");
        }

        return switch (writers.size()) {
            case 1 ->
                findByName(DEFAULT_BLOG_TEMPLATE)
                        .orElseThrow(() -> new IllegalStateException("Template introuvable: " + DEFAULT_BLOG_TEMPLATE));
            case 2 ->
                findByName(DEFAULT_BLOG_DUO_TEMPLATE)
                        .orElseThrow(
                                () -> new IllegalStateException("Template introuvable: " + DEFAULT_BLOG_DUO_TEMPLATE));
            default ->
                throw new IllegalArgumentException(
                        "Le nombre d'auteurs pour un blog doit être compris entre 1 et 2 (reçu : " + writers.size()
                                + ").");
        };
    }

    public VisualTemplate resolveNightClazzTemplate(NightClazzVisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de visuel NightClazz ne peut pas être nulle.");
        }
        List<PersonInput> speakers = sanitizeList(request.speakers());
        if (speakers.isEmpty()) {
            throw new IllegalArgumentException("Au moins un speaker est requis pour un visuel NightClazz.");
        }

        boolean hasPartner =
                request.partner() != null && isNotBlank(request.partner().name());
        if (hasPartner) {
            if (speakers.size() != 1) {
                throw new IllegalArgumentException(
                        "Le template NightClazz partenaire ne supporte qu'un seul speaker (reçu : " + speakers.size()
                                + ").");
            }
            return findByName(DEFAULT_NIGHTCLAZZ_PARTNER_TEMPLATE)
                    .orElseThrow(() ->
                            new IllegalStateException("Template introuvable: " + DEFAULT_NIGHTCLAZZ_PARTNER_TEMPLATE));
        }

        if (speakers.size() > 2) {
            throw new IllegalArgumentException(
                    "Le nombre de speakers pour un NightClazz doit être compris entre 1 et 2 (reçu : " + speakers.size()
                            + ").");
        }
        return findByName(DEFAULT_NIGHTCLAZZ_TEMPLATE)
                .orElseThrow(() -> new IllegalStateException("Template introuvable: " + DEFAULT_NIGHTCLAZZ_TEMPLATE));
    }

    public InlineMediaPart loadTemplateImage(VisualTemplate template) {
        String templatePath = template.templatePath();
        String mimeType = detectMimeType(templatePath);

        if (customTemplatesDir.isPresent() && !customTemplatesDir.get().isBlank()) {
            Path externalFile = Path.of(customTemplatesDir.get().trim()).resolve(templatePath);
            if (Files.exists(externalFile)) {
                try {
                    return new InlineMediaPart(Files.readAllBytes(externalFile), mimeType);
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "Impossible de lire le fichier de template externe : " + externalFile, e);
                }
            }
        }

        String resourcePath = templatePath.startsWith("/") ? templatePath : "/" + templatePath;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Fichier d'image du template introuvable : " + templatePath);
            }
            return new InlineMediaPart(is.readAllBytes(), mimeType);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger l'image du template : " + templatePath, e);
        }
    }

    public String buildTalkPrompt(VisualTemplate template, TalkVisualRequest request) {
        List<TalkItemInput> talks = sanitizeTalks(request.talks());
        List<PersonInput> speakers = sanitizeList(request.speakers());

        String firstTitle = !talks.isEmpty() ? safeTrim(talks.get(0).title()) : safeTrim(request.title());
        String secondTitle = talks.size() > 1 ? safeTrim(talks.get(1).title()) : "";

        String firstSpeaker = !talks.isEmpty()
                ? safeTrim(talks.get(0).speaker().name())
                : (speakers.size() > 0 ? safeTrim(speakers.get(0).name()) : "");
        String secondSpeaker = talks.size() > 1
                ? safeTrim(talks.get(1).speaker().name())
                : (speakers.size() > 1 ? safeTrim(speakers.get(1).name()) : "");
        String thirdSpeaker = speakers.size() > 2 ? safeTrim(speakers.get(2).name()) : "";

        Map<String, String> values = new HashMap<>();
        values.put("%TEMPLATE%", template.templatePath());
        values.put("%TITLE%", firstTitle);
        values.put("%TITLE2%", secondTitle);
        values.put(
                "%CONFERENCE%",
                request.conference() != null ? safeTrim(request.conference().name()) : "");
        values.put("%NAME%", firstSpeaker);
        values.put("%NAME2%", secondSpeaker);
        values.put("%NAME3%", thirdSpeaker);
        values.put("%PHOTO%", "speaker-photo-1");
        values.put("%PHOTO2%", "speaker-photo-2");
        values.put("%PHOTO3%", "speaker-photo-3");
        values.put("%CONF_PHOTO%", "conference-logo");

        return replacePlaceholders(template.prompt(), values);
    }

    public String buildBlogPrompt(VisualTemplate template, BlogVisualRequest request) {
        List<PersonInput> writers = sanitizeList(request.writers());
        Map<String, String> values = new HashMap<>();
        values.put("%TEMPLATE%", template.templatePath());
        values.put("%TITLE%", safeTrim(request.title()));
        values.put("%NAME%", writers.size() > 0 ? safeTrim(writers.get(0).name()) : "");
        values.put("%NAME2%", writers.size() > 1 ? safeTrim(writers.get(1).name()) : "");
        values.put("%PHOTO%", "author-photo-1");
        values.put("%PHOTO2%", "author-photo-2");

        return replacePlaceholders(template.prompt(), values);
    }

    public String buildNightClazzPrompt(VisualTemplate template, NightClazzVisualRequest request) {
        List<PersonInput> speakers = sanitizeList(request.speakers());
        Map<String, String> values = new HashMap<>();
        values.put("%TEMPLATE%", template.templatePath());
        values.put("%TITLE%", safeTrim(request.title()));
        values.put("%DATE%", isNotBlank(request.date()) ? safeTrim(request.date()) : "");
        values.put("%HOUR%", isNotBlank(request.hour()) ? safeTrim(request.hour()) : "18h30-21h");
        values.put("%LOCATION%", isNotBlank(request.location()) ? safeTrim(request.location()) : "Zenika Nantes");
        values.put("%NAME%", speakers.size() > 0 ? safeTrim(speakers.get(0).name()) : "");
        values.put("%NAME2%", speakers.size() > 1 ? safeTrim(speakers.get(1).name()) : "");
        values.put("%PHOTO%", "speaker-photo-1");
        values.put("%PHOTO2%", "speaker-photo-2");
        values.put(
                "%PARTNER%",
                request.partner() != null ? safeTrim(request.partner().name()) : "");
        values.put("%PARTNER_LOGO%", "partner-logo");

        return replacePlaceholders(template.prompt(), values);
    }

    public String detectMimeType(String filePath) {
        if (filePath == null || !filePath.contains(".")) {
            return "image/png";
        }
        String ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "image/png";
        };
    }

    private String replacePlaceholders(String promptTemplate, Map<String, String> values) {
        String resolved = promptTemplate;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return resolved;
    }

    private static List<PersonInput> sanitizeList(List<PersonInput> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().filter(p -> p != null && isNotBlank(p.name())).toList();
    }

    private static List<TalkItemInput> sanitizeTalks(List<TalkItemInput> talks) {
        if (talks == null) {
            return List.of();
        }
        return talks.stream()
                .filter(t -> t != null
                        && isNotBlank(t.title())
                        && t.speaker() != null
                        && isNotBlank(t.speaker().name()))
                .toList();
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
