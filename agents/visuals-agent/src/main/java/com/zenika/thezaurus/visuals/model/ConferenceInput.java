package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zenika.thezaurus.visuals.model.deserializer.ConferenceInputDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonDeserialize(using = ConferenceInputDeserializer.class)
public record ConferenceInput(String name, String logoBase64) {

    public ConferenceInput(String name) {
        this(name, null);
    }
}
