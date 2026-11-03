package com.zenika.thezaurus.visuals.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zenika.thezaurus.visuals.model.deserializer.PartnerInputDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonDeserialize(using = PartnerInputDeserializer.class)
public record PartnerInput(String name, String logoBase64) {

    public PartnerInput(String name) {
        this(name, null);
    }
}
