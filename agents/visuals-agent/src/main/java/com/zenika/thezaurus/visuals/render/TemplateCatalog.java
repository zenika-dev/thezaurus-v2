package com.zenika.thezaurus.visuals.render;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TemplateCatalog {
    private final List<TemplateSpec> entries;
    private final Map<String, TemplateSpec> byId;

    public TemplateCatalog() {
        this(load());
    }

    /** Explicit catalog constructor keeps rendering independently testable. */
    public TemplateCatalog(List<TemplateSpec> entries) {
        this.entries = List.copyOf(entries);
        Map<String, TemplateSpec> index = new LinkedHashMap<>();
        for (TemplateSpec entry : entries) {
            if (index.put(entry.id(), entry) != null)
                throw new IllegalArgumentException("Template dupliqué : " + entry.id());
        }
        this.byId = Map.copyOf(index);
    }

    public List<TemplateSpec> list() {
        return entries;
    }

    public TemplateSpec require(String id) {
        TemplateSpec spec = byId.get(id);
        if (spec == null) throw new IllegalArgumentException("Template inconnu : " + id);
        return spec;
    }

    private static List<TemplateSpec> load() {
        try (InputStream stream = TemplateCatalog.class.getResourceAsStream("/templates.json")) {
            if (stream == null) throw new IllegalStateException("Catalogue de templates introuvable.");
            return new ObjectMapper().readValue(stream, new TypeReference<List<TemplateSpec>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger le catalogue de templates.", e);
        }
    }
}
