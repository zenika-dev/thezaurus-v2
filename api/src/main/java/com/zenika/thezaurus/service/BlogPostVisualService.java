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
public class BlogPostVisualService {
    @Inject
    BlogPostService posts;

    @Inject
    ProfilePhotoService photos;

    @Inject
    VisualAgentClient agent;

    public byte[] generate(String id) throws ExecutionException, InterruptedException {
        agent.requireConfigured();
        var post = posts.findById(id);
        if (post == null) throw new NotFoundException();
        if (post.getWriters() == null
                || post.getWriters().isEmpty()
                || post.getWriters().size() > 2) {
            throw new BadRequestException("Un ou deux auteurs sont requis.");
        }
        var writers = new ArrayList<Map<String, Object>>();
        for (var writer : post.getWriters()) {
            if (writer == null || writer.email() == null || writer.email().isBlank()) {
                throw new BadRequestException("Chaque auteur doit avoir un profil avec une photo autorisée pour l’IA.");
            }
            try {
                var photo = photos.getForAgent(writer.email());
                writers.add(Map.of(
                        "name",
                        writer.name(),
                        "photo",
                        Map.of("base64", Base64.getEncoder().encodeToString(photo.bytes()))));
            } catch (ForbiddenException denied) {
                // A missing photo selects the local Zenika logo, without sending private pixels to the agent.
                writers.add(Map.of("name", writer.name()));
            }
        }
        return agent.generate("blogs", Map.of("title", post.getTitle(), "writers", writers, "autoCrop", true));
    }
}
