package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RegisterForReflection
public record TalkVisualRequest(
        @NotBlank(message = "Le titre du talk est requis") String title,
        @NotNull(message = "La conférence est requise") ConferenceInput conference,

        @NotEmpty(message = "Au moins un speaker est requis") @JsonAlias("speaker")
        List<PersonInput> speakers) {

    public TalkVisualRequest(String title, String conferenceName, List<PersonInput> speakers) {
        this(title, new ConferenceInput(conferenceName), speakers);
    }
}
