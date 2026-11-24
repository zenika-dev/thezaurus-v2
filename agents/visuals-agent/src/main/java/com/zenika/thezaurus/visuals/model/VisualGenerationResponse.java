package com.zenika.thezaurus.visuals.model;

import com.zenika.thezaurus.visuals.crop.AutoCropService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.List;

@RegisterForReflection
public record VisualGenerationResponse(
        VisualType category,
        String templateName,
        String model,
        String mimeType,
        String imageBase64,
        String promptUsed,
        Instant generatedAt,
        int width,
        int height,
        List<AutoCropService.CropResult> crops,
        Usage usage) {
    public record Usage(int calls, long inputTokens, long outputTokens, long totalTokens, int cacheHits) {}
}
