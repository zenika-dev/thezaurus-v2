package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotBlank;

public record CityCount(@NotBlank String city, int count) {}
