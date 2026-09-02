package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TalkReviewRequest(
        String title, @JsonProperty("abstract") String abstractText) {}
