package com.zenika.thezaurus.model;

import java.util.List;

/** Metadata and API entry point for an editable email template with a talk preview. */
public record MessageTemplateDefinition(
        String id,
        String label,
        String description,
        String apiPath,
        List<TemplateToken> variables,
        List<TemplateToken> conditions,
        List<TemplateLink> links,
        String example,
        String help) {
    public record TemplateToken(String name, String label) {}

    public record TemplateLink(String variable, String label, String text) {}
}
