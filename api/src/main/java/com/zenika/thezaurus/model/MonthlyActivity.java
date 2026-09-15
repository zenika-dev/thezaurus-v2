package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotNull;

public record MonthlyActivity(@NotNull MonthLabel month, int internal, int external) {}
