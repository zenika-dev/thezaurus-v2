package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.MessageTemplateDefinition.TemplateToken;
import java.util.List;

public final class ReminderTemplateMetadata {
    private ReminderTemplateMetadata() {}

    public static final List<TemplateToken> CONDITIONS = List.of(
            new TemplateToken("hasConference", "Conférence renseignée"),
            new TemplateToken("hasDate", "Date renseignée"),
            new TemplateToken("missingVideo", "Vidéo manquante"),
            new TemplateToken("missingAudience", "Audience manquante"));
}
