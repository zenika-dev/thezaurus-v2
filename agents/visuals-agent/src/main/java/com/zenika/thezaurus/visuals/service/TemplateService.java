package com.zenika.thezaurus.visuals.service;

import com.zenika.thezaurus.visuals.model.*;
import com.zenika.thezaurus.visuals.render.TemplateCatalog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TemplateService {
    private static final Map<String, String> NAMES = Map.of(
            "conf-1",
            "generate-image-speaker-event",
            "conf-1-binome",
            "generate-image-2-speaker-event",
            "conf-1-trinome",
            "generate-image-3-speaker-event",
            "conf-2",
            "generate-image-2-talks-event",
            "blog",
            "generate-image-blog-post",
            "blog-2",
            "generate-image-2-blog-post",
            "nc",
            "generate-image-nightclazz",
            "nc-partenaire",
            "generate-image-nightclazz-partner");

    @Inject
    TemplateCatalog catalog;

    public List<VisualTemplate> listTemplates(VisualType category) {
        return catalog.list().stream()
                .map(spec -> new VisualTemplate(
                        NAMES.get(spec.id()),
                        spec.label(),
                        type(spec.id()),
                        spec.background(),
                        fields(spec.id()),
                        null))
                .filter(t -> category == null || t.category() == category)
                .toList();
    }

    public String name(String id) {
        return NAMES.get(id);
    }

    private static List<String> fields(String id) {
        if ("conf-2".equals(id)) return List.of("conference", "talks", "autoCrop");
        return switch (type(id)) {
            case TALK -> List.of("title", "conference", "speakers", "autoCrop");
            case BLOG -> List.of("title", "writers", "autoCrop");
            case NIGHT_CLAZZ ->
                "nc-partenaire".equals(id)
                        ? List.of("title", "date", "hour", "location", "speakers", "partner", "autoCrop")
                        : List.of("title", "date", "hour", "location", "speakers", "autoCrop");
        };
    }

    public static VisualType type(String id) {
        return id.startsWith("conf")
                ? VisualType.TALK
                : id.startsWith("blog") ? VisualType.BLOG : VisualType.NIGHT_CLAZZ;
    }
}
