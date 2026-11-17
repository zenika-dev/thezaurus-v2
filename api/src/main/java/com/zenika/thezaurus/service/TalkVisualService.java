package com.zenika.thezaurus.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class TalkVisualService {
    @Inject
    TalkService talks;

    @Inject
    ProfilePhotoService photos;

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
        var speakers = new ArrayList<Map<String, Object>>();
        for (var speaker : talk.speakers()) {
            if (speaker == null || speaker.email() == null || speaker.email().isBlank()) {
                throw new BadRequestException(
                        "Chaque intervenant doit avoir un profil avec une photo autorisée pour l’IA.");
            }
            try {
                var photo = photos.getForAgent(speaker.email());
                speakers.add(Map.of(
                        "name",
                        speaker.name(),
                        "photo",
                        Map.of("base64", Base64.getEncoder().encodeToString(photo.bytes()))));
            } catch (ForbiddenException denied) {
                // A missing photo selects the local Zenika logo, without sending private pixels to the agent.
                speakers.add(Map.of("name", speaker.name()));
            }
        }
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
