package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zenika.thezaurus.visuals.model.deserializer.PersonInputDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonDeserialize(using = PersonInputDeserializer.class)
public record PersonInput(String name, String photoBase64) {

    public PersonInput(String name) {
        this(name, null);
    }
}
