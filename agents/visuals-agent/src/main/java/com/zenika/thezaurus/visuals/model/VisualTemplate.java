package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record VisualTemplate(
        String name,
        String description,
        VisualType category,
        String templatePath,
        List<String> fields,
        String prompt) {}
