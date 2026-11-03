package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record TalkItemInput(String title, PersonInput speaker) {

    public TalkItemInput(String title, String speakerName) {
        this(title, new PersonInput(speakerName));
    }
}
