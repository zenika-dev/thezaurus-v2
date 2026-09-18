package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReminderTemplatePreview(
        @NotNull String subject,
        @NotNull String bodyHtml,
        @NotNull List<String> to) {}
