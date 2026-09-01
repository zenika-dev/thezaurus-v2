package com.zenika.thezaurus.model;

import java.util.List;

public record EventsDashboard(
        int year, EventsTotals totals, List<MonthlyActivity> monthly, List<EventTypeSummary> eventTypes) {}
