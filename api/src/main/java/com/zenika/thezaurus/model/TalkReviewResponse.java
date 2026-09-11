package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TalkReviewResponse(
        @NotNull List<String> suggestedTitles,
        @NotNull List<String> suggestedAbstracts,
        @NotNull List<String> feedback,
        @NotNull List<String> keyImprovements) {}
