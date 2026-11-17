package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Resolves photos privately, then checks all participants again after the last remote read. */
@ApplicationScoped
public class VisualParticipantsService {
    @Inject
    ProfilePhotoService photos;

    public List<Map<String, Object>> prepare(List<User> people) throws ExecutionException, InterruptedException {
        var result = new ArrayList<Map<String, Object>>();
        for (var person : people) {
            if (person == null || person.email() == null || person.email().isBlank()) {
                throw new BadRequestException("Chaque participant doit avoir un profil.");
            }
            // A missing profile is an error, unlike a missing or refused photograph.
            photos.requireUser(person.email());
            var entry = new HashMap<String, Object>();
            entry.put("name", person.name());
            try {
                var photo = photos.getForAgent(person.email());
                entry.put("photo", Map.of("base64", Base64.getEncoder().encodeToString(photo.bytes())));
            } catch (ForbiddenException | NotFoundException noPhoto) {
                // Omit the pixels: the renderer uses its local Zenika logo.
            }
            result.add(entry);
        }
        // No image downloads remain after this check. A deletion still returns 404.
        for (int i = 0; i < people.size(); i++) {
            if (!photos.requireUser(people.get(i).email()).allowsAiPhoto())
                result.get(i).remove("photo");
        }
        return result;
    }
}
