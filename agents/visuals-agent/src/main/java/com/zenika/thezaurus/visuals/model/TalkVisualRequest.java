package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RegisterForReflection
public record TalkVisualRequest(
        String title,
        @NotNull(message = "La conférence est requise") ConferenceInput conference,

        @JsonAlias("speaker") List<PersonInput> speakers,
        List<TalkInput> talks,
        Boolean autoCrop) {

    public record TalkInput(
            String title, @JsonAlias("speaker") List<PersonInput> speakers) {}

    public TalkVisualRequest(String title, ConferenceInput conference, List<PersonInput> speakers) {
        this(title, conference, speakers, null, null);
    }

    public TalkVisualRequest(String title, String conferenceName, List<PersonInput> speakers) {
        this(title, new ConferenceInput(conferenceName), speakers);
    }
}
