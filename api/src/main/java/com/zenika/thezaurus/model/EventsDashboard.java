package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EventsDashboard(
        int year,
        @NotNull EventsTotals totals,
        @NotNull List<MonthlyActivity> monthly,
        @NotNull List<EventTypeSummary> eventTypes) {}
