package com.zenika.thezaurus.model;

import java.util.List;

/**
 * Les six derniers champs existaient cote formulaire sans contrepartie ici : ils etaient
 * saisissables mais jamais persistes, et relus a vide au rafraichissement suivant.
 *
 * @param format format de presentation (conference, atelier…), saisi a la creation
 * @param date date de presentation au format ISO {@code YYYY-MM-DD}
 * @param language langue de presentation, saisie a la creation
 * @param notes notes libres saisies a la creation
 * @param slides lien vers les supports, saisi apres coup depuis la fiche du talk
 * @param replay lien vers l'enregistrement, saisi apres coup depuis la fiche du talk
 */
public record Talk(
        String id,
        String title,
        String description,
        List<User> speakers,
        String office,
        Conference conference,
        TalkStatus status,
        Visibility visibility,
        String format,
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
