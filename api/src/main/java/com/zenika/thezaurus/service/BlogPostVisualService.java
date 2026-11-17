package com.zenika.thezaurus.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class BlogPostVisualService {
    @Inject
    BlogPostService posts;

    @Inject
    VisualParticipantsService participants;

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
        var writers = participants.prepare(post.getWriters());
        return agent.generate("blogs", Map.of("title", post.getTitle(), "writers", writers, "autoCrop", true));
    }
}
