package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@RegisterForReflection
public record VisualStreamEvent(Status status, String message, VisualGenerationResponse result, Instant timestamp) {

    @RegisterForReflection
    public enum Status {
        STARTED,
        TEMPLATE_LOADED,
        GENERATING,
        HEARTBEAT,
        COMPLETED,
        FAILED
    }

    public static VisualStreamEvent of(Status status, String message) {
        return new VisualStreamEvent(status, message, null, Instant.now());
    }

    public static VisualStreamEvent completed(String message, VisualGenerationResponse result) {
        return new VisualStreamEvent(Status.COMPLETED, message, result, Instant.now());
    }
}
