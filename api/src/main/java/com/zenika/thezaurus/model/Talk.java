package com.zenika.thezaurus.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Représentation d'un talk.
 *
 * @param date date de presentation au format ISO {@code YYYY-MM-DD}
 * @param slides lien vers les supports, saisi depuis la fiche du talk
 * @param replay lien vers l'enregistrement, saisi depuis la fiche du talk
 * @param audience nombre constaté ou estimé de participants ayant assisté au talk
 */
public record Talk(
        String id,
        @NotBlank String title,
        @NotBlank String description,
        @NotEmpty List<@NotNull @Valid User> speakers,
        @NotBlank String office,
        Conference conference,
        @NotNull TalkStatus status,
        @NotNull Visibility visibility,

        @NotBlank
        @Schema(
                anyOf = {TalkFormat.class, String.class},
                description = "Format proposé par TalkFormat, ou ancienne valeur conservée sans modification")
        String format,

        String date,
        String language,
        String notes,

        @Pattern(regexp = "^$|https?://[^\\s]+", message = "Une URL HTTP ou HTTPS est requise")
        String slides,

        @Pattern(regexp = "^$|https?://[^\\s]+", message = "Une URL HTTP ou HTTPS est requise")
        String replay,

        @Min(0) Integer audience) {

    public boolean canBeEditedBy(String email, boolean privileged) {
        return privileged
                || (email != null
                        && !email.isBlank()
                        && speakers != null
                        && speakers.stream()
                                .anyMatch(user -> user != null
                                        && user.email() != null
                                        && user.email().trim().equalsIgnoreCase(email.trim())));
    }

    public Talk withDate(String date) {
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
                replay,
                audience);
    }

    public Talk(String id, String title, String description) {
        this(id, title, description, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Talk(
            String title,
            String description,
            List<User> speakers,
            String office,
            TalkStatus status,
            Visibility visibility) {
        this(
                null,
                title,
                description,
                speakers,
                office,
                null,
                status,
                visibility,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
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
                replay,
                audience);
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
                replay,
                audience);
    }
}
