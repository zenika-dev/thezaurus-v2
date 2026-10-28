package com.zenika.thezaurus.visuals.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.TalkVisualRequest;
import com.zenika.thezaurus.visuals.model.VisualRequest;
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
    public static final String DEFAULT_BLOG_TEMPLATE = "generate-image-blog-post";
    public static final String DEFAULT_BLOG_DUO_TEMPLATE = "generate-image-2-blog-post";

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

    /**
     * Sélectionne automatiquement le bon template en fonction des données en entrée :
     * <ul>
     *   <li>Type ("talk" ou "blog")</li>
     *   <li>Conférence (requise pour un talk)</li>
     *   <li>Nombre de speakers (1, 2 ou 3)</li>
     *   <li>Nombre de rédacteurs / writers (1 ou 2)</li>
     * </ul>
     */
    public VisualTemplate resolveTemplate(VisualRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de visuel ne peut pas être nulle.");
        }

        VisualType category = determineCategory(request);
        return switch (category) {
            case TALK -> resolveTalkTemplate(request);
            case BLOG -> resolveBlogTemplate(request);
        };
    }

    public VisualType determineCategory(VisualRequest request) {
        String type = request.type();
        if (type != null && !type.isBlank()) {
            String normalized = type.trim();
            if (normalized.equalsIgnoreCase("talk")) {
                return VisualType.TALK;
            }
            if (normalized.equalsIgnoreCase("blog")) {
                return VisualType.BLOG;
            }
            throw new IllegalArgumentException(
                    "Type de visuel inconnu : '" + type + "'. Les valeurs supportées sont 'talk' ou 'blog'.");
        }

        // Déduction automatique si le champ 'type' n'est pas explicite
        boolean hasConference =
                request.conference() != null && isNotBlank(request.conference().name());
        boolean hasSpeakers = request.speakers() != null && !request.speakers().isEmpty();
        boolean hasWriters = request.writers() != null && !request.writers().isEmpty();

        if ((hasConference || hasSpeakers) && !hasWriters) {
            return VisualType.TALK;
        }
        if (hasWriters && !hasConference && !hasSpeakers) {
            return VisualType.BLOG;
        }

        throw new IllegalArgumentException(
                "Impossible de déduire le type de visuel ('talk' ou 'blog'). Veuillez renseigner le champ 'type'.");
    }

    public VisualTemplate resolveTalkTemplate(VisualRequest request) {
        if (request.conference() == null || !isNotBlank(request.conference().name())) {
            throw new IllegalArgumentException("Le nom de la conférence est requis pour un visuel de talk.");
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

    public VisualTemplate resolveBlogTemplate(VisualRequest request) {
        List<PersonInput> writers = sanitizeList(request.writers());
        if (writers.isEmpty()) {
            writers = sanitizeList(request.speakers());
        }

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

    public VisualTemplate resolveTalkTemplate(TalkVisualRequest legacyRequest) {
        return resolveTalkTemplate(legacyRequest.toVisualRequest());
    }

    public VisualTemplate resolveBlogTemplate(BlogVisualRequest legacyRequest) {
        return resolveBlogTemplate(legacyRequest.toVisualRequest());
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

    public String buildPrompt(VisualTemplate template, VisualRequest request) {
        if (template.category() == VisualType.TALK) {
            return buildTalkPrompt(template, request);
        } else {
            return buildBlogPrompt(template, request);
        }
    }

    public String buildTalkPrompt(VisualTemplate template, VisualRequest request) {
        List<PersonInput> speakers = sanitizeList(request.speakers());
        Map<String, String> values = new HashMap<>();
        values.put("%TEMPLATE%", template.templatePath());
        values.put("%TITLE%", safeTrim(request.title()));
        values.put(
                "%CONFERENCE%",
                request.conference() != null ? safeTrim(request.conference().name()) : "");
        values.put("%NAME%", speakers.size() > 0 ? safeTrim(speakers.get(0).name()) : "");
        values.put("%NAME2%", speakers.size() > 1 ? safeTrim(speakers.get(1).name()) : "");
        values.put("%NAME3%", speakers.size() > 2 ? safeTrim(speakers.get(2).name()) : "");
        values.put("%PHOTO%", "speaker-photo-1");
        values.put("%PHOTO2%", "speaker-photo-2");
        values.put("%PHOTO3%", "speaker-photo-3");
        values.put("%CONF_PHOTO%", "conference-logo");

        return replacePlaceholders(template.prompt(), values);
    }

    public String buildBlogPrompt(VisualTemplate template, VisualRequest request) {
        List<PersonInput> writers = sanitizeList(request.writers());
        if (writers.isEmpty()) {
            writers = sanitizeList(request.speakers());
        }
        Map<String, String> values = new HashMap<>();
        values.put("%TEMPLATE%", template.templatePath());
        values.put("%TITLE%", safeTrim(request.title()));
        values.put("%NAME%", writers.size() > 0 ? safeTrim(writers.get(0).name()) : "");
        values.put("%NAME2%", writers.size() > 1 ? safeTrim(writers.get(1).name()) : "");
        values.put("%PHOTO%", "author-photo-1");
        values.put("%PHOTO2%", "author-photo-2");

        return replacePlaceholders(template.prompt(), values);
    }

    public String buildTalkPrompt(VisualTemplate template, TalkVisualRequest legacyRequest) {
        return buildTalkPrompt(template, legacyRequest.toVisualRequest());
    }

    public String buildBlogPrompt(VisualTemplate template, BlogVisualRequest legacyRequest) {
        return buildBlogPrompt(template, legacyRequest.toVisualRequest());
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

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
