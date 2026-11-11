package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zenika.thezaurus.visuals.model.deserializer.PersonInputDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonDeserialize(using = PersonInputDeserializer.class)
public record PersonInput(String name, PhotoInput photo) {

    public PersonInput(String name) {
        this(name, (PhotoInput) null);
    }

    public PersonInput(String name, String photoBase64) {
        this(name, photoBase64 != null && !photoBase64.isBlank() ? PhotoInput.ofBase64(photoBase64) : null);
    }
}
