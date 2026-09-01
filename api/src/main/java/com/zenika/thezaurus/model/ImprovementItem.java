package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImprovementItem(
        String category,
        String comment,
        List<String> suggestions
) {}