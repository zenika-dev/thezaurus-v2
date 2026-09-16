package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotNull;

public record ReminderTemplateView(
        @NotNull String subject,
        @NotNull String bodyHtml,
        @NotNull long revision) {}
