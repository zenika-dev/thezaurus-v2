package com.zenika.thezaurus.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Wire contract for the two image endpoints consumed by TheZaurus. */
public sealed interface VisualAgentRequest {
    record Talk(String title, String conference, List<Person> speakers, boolean autoCrop)
            implements VisualAgentRequest {}

    record Blog(String title, List<Person> writers, boolean autoCrop) implements VisualAgentRequest {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Person(String name, Photo photo) {}

    record Photo(String base64) {}
}
