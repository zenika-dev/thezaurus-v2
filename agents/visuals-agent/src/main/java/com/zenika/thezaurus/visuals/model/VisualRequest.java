package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RegisterForReflection
public record VisualRequest(
        @NotBlank(message = "Le titre est requis") String title,
        String type,
        ConferenceInput conference,
        List<PersonInput> speakers,
        List<PersonInput> writers) {

    public static VisualRequest forTalk(String title, String conferenceName, List<PersonInput> speakers) {
        return new VisualRequest(title, "talk", new ConferenceInput(conferenceName), speakers, null);
    }

    public static VisualRequest forTalk(String title, ConferenceInput conference, List<PersonInput> speakers) {
        return new VisualRequest(title, "talk", conference, speakers, null);
    }

    public static VisualRequest forBlog(String title, List<PersonInput> writers) {
        return new VisualRequest(title, "blog", (ConferenceInput) null, null, writers);
    }

    public VisualRequest withType(String newType) {
        return new VisualRequest(title, newType, conference, speakers, writers);
    }
}
