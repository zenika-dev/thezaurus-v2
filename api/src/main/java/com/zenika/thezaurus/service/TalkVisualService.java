package com.zenika.thezaurus.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class TalkVisualService {
    @Inject
    TalkService talks;

    @Inject
    VisualParticipantsService participants;

    @Inject
    VisualAgentClient agent;

    public byte[] generate(String id) throws ExecutionException, InterruptedException {
        agent.requireConfigured();
        var talk = talks.findById(id);
        if (talk == null) throw new NotFoundException();
        if (talk.speakers() == null
                || talk.speakers().isEmpty()
                || talk.speakers().size() > 3
                || talk.conference() == null
                || talk.conference().getName() == null
                || talk.conference().getName().isBlank()) {
            throw new BadRequestException("Un à trois intervenants et une conférence sont requis.");
        }
        var speakers = participants.prepare(talk.speakers());
        return agent.generate(
                "talks",
                Map.of(
                        "title",
                        talk.title(),
                        "conference",
                        talk.conference().getName(),
                        "speakers",
                        speakers,
                        "autoCrop",
                        true));
    }
}
