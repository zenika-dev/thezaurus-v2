package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EventTypeSummary(
        @NotBlank String name,
        @NotBlank String visibility,
        int total,
        @NotNull List<CityCount> cities) {}
