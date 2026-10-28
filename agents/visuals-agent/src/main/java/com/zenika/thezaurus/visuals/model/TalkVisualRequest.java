package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public record TalkVisualRequest(
        @NotBlank(message = "Le titre du talk est requis") String title,
        @NotBlank(message = "Le nom du speaker est requis") String speakerName,
        String secondSpeakerName,
        String thirdSpeakerName,

        @NotBlank(message = "Le nom de la conférence visée est requis")
        String conferenceName,

        String templateName,
        String speakerPhotoBase64,
        String secondSpeakerPhotoBase64,
        String thirdSpeakerPhotoBase64,
        String conferenceLogoBase64) {

    public TalkVisualRequest(String title, String speakerName, String conferenceName, String templateName) {
        this(title, speakerName, null, null, conferenceName, templateName, null, null, null, null);
    }

    public VisualRequest toVisualRequest() {
        List<PersonInput> speakers = new ArrayList<>();
        if (speakerName != null && !speakerName.isBlank()) {
            speakers.add(new PersonInput(speakerName, speakerPhotoBase64));
        }
        if (secondSpeakerName != null && !secondSpeakerName.isBlank()) {
            speakers.add(new PersonInput(secondSpeakerName, secondSpeakerPhotoBase64));
        }
        if (thirdSpeakerName != null && !thirdSpeakerName.isBlank()) {
            speakers.add(new PersonInput(thirdSpeakerName, thirdSpeakerPhotoBase64));
        }
        return new VisualRequest(
                title, "talk", new ConferenceInput(conferenceName, conferenceLogoBase64), speakers, null);
    }
}
