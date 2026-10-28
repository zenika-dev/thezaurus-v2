package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public record VisualGenerationResponse(
        VisualType category,
        String templateName,
        String model,
        String mimeType,
        String imageBase64,
        String promptUsed,
        Instant generatedAt) {}
