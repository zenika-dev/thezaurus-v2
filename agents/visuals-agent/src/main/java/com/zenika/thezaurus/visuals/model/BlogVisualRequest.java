package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public record BlogVisualRequest(
        @NotBlank(message = "Le titre du blog post est requis")
        String title,

        @NotBlank(message = "Le nom de l'auteur est requis") String authorName,
        String secondAuthorName,
        String templateName,
        String authorPhotoBase64,
        String secondAuthorPhotoBase64) {

    public BlogVisualRequest(String title, String authorName, String templateName) {
        this(title, authorName, null, templateName, null, null);
    }

    public VisualRequest toVisualRequest() {
        List<PersonInput> writers = new ArrayList<>();
        if (authorName != null && !authorName.isBlank()) {
            writers.add(new PersonInput(authorName, authorPhotoBase64));
        }
        if (secondAuthorName != null && !secondAuthorName.isBlank()) {
            writers.add(new PersonInput(secondAuthorName, secondAuthorPhotoBase64));
        }
        return new VisualRequest(title, "blog", (ConferenceInput) null, null, writers);
    }
}
