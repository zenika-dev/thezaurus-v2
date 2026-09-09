package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TalkReviewRequest(
        @NotBlank String title,
        @NotBlank @JsonProperty("abstract") String abstractText) {}
