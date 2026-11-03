package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@RegisterForReflection
public record NightClazzVisualRequest(
        @NotBlank(message = "Le titre du NightClazz est requis")
        String title,

        String date,
        String hour,
        String location,

        @NotEmpty(message = "Au moins un speaker est requis") @JsonAlias("speaker")
        List<PersonInput> speakers,

        PartnerInput partner) {

    public NightClazzVisualRequest(
            String title, String date, String hour, String location, List<PersonInput> speakers) {
        this(title, date, hour, location, speakers, null);
    }

    public static NightClazzVisualRequest withPartner(
            String title, String date, String hour, String location, PersonInput speaker, PartnerInput partner) {
        return new NightClazzVisualRequest(
                title, date, hour, location, speaker != null ? List.of(speaker) : List.of(), partner);
    }
}
