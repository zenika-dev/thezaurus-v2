package com.zenika.thezaurus.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Représentation d'un talk.
 *
 * @param date date de presentation au format ISO {@code YYYY-MM-DD}
 * @param slides lien vers les supports, saisi depuis la fiche du talk
 * @param replay lien vers l'enregistrement, saisi depuis la fiche du talk
 */
public record Talk(
        String id,
        @NotBlank String title,
        @NotBlank String description,
        List<User> speakers,
        @NotBlank String office,
        Conference conference,
        @NotNull TalkStatus status,
        @NotNull Visibility visibility,
        @NotBlank String format,
        String date,
        String language,
        String notes,
        String slides,
        String replay) {

    public Talk(String id, String title, String description) {
        this(id, title, description, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Talk(
            String title,
            String description,
            List<User> speakers,
            String office,
            TalkStatus status,
            Visibility visibility) {
        this(null, title, description, speakers, office, null, status, visibility, null, null, null, null, null, null);
    }

    public Talk withId(String id) {
        return new Talk(
                id,
                title,
                description,
                speakers,
                office,
                conference,
                status,
                visibility,
                format,
                date,
                language,
                notes,
                slides,
                replay);
    }

    public Talk withConference(Conference conference) {
        return new Talk(
                id,
                title,
                description,
                speakers,
                office,
                conference,
                status,
                visibility,
                format,
                date,
                language,
                notes,
                slides,
                replay);
    }
}
