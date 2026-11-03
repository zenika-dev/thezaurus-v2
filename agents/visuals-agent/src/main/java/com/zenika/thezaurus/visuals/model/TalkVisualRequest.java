package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RegisterForReflection
public record TalkVisualRequest(
        String title,
        @NotNull(message = "La conférence est requise") ConferenceInput conference,
        @JsonAlias("speaker") List<PersonInput> speakers,
        List<TalkItemInput> talks) {

    public TalkVisualRequest(String title, ConferenceInput conference, List<PersonInput> speakers) {
        this(title, conference, speakers, null);
    }

    public TalkVisualRequest(String title, String conferenceName, List<PersonInput> speakers) {
        this(title, new ConferenceInput(conferenceName), speakers, null);
    }

    public static TalkVisualRequest forTwoTalks(ConferenceInput conference, List<TalkItemInput> talks) {
        String firstTitle = talks != null && !talks.isEmpty() && talks.get(0) != null
                ? talks.get(0).title()
                : null;
        return new TalkVisualRequest(firstTitle, conference, null, talks);
    }

    @JsonIgnore
    @AssertTrue(message = "Le titre du talk est requis")
    public boolean isTitleValid() {
        if (title != null && !title.isBlank()) {
            return true;
        }
        return talks != null
                && !talks.isEmpty()
                && talks.get(0) != null
                && talks.get(0).title() != null
                && !talks.get(0).title().isBlank();
    }
}
