package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Resolves photos privately, then checks all participants again after the last remote read. */
@ApplicationScoped
public class VisualParticipantsService {
    @Inject
    ProfilePhotoService photos;

    public List<VisualAgentRequest.Person> prepare(List<User> people) throws ExecutionException, InterruptedException {
        return prepare(people, false);
    }

    public List<VisualAgentRequest.Person> prepareAuthors(List<User> people)
            throws ExecutionException, InterruptedException {
        return prepare(people, true);
    }

    private List<VisualAgentRequest.Person> prepare(List<User> people, boolean allowFreeAuthors)
            throws ExecutionException, InterruptedException {
        var result = new ArrayList<VisualAgentRequest.Person>();
        for (var person : people) {
            if (person == null) throw new BadRequestException("Participant invalide.");
            var user = profile(person.email(), allowFreeAuthors);
            var name = person.name();
            if (name == null || name.isBlank()) name = user == null ? null : user.name();
            if (name == null || name.isBlank()) throw new BadRequestException("Chaque participant doit avoir un nom.");
            VisualAgentRequest.Photo photo = null;
            if (user != null) {
                try {
                    var image = photos.getForRendering(person.email());
                    photo = new VisualAgentRequest.Photo(Base64.getEncoder().encodeToString(image.bytes()));
                } catch (NotFoundException noPhoto) {
                    // Omit the pixels: the renderer uses its local Zenika logo.
                }
            }
            result.add(new VisualAgentRequest.Person(name, photo, photo != null && user.allowsAiPhoto()));
        }
        for (int i = 0; i < people.size(); i++) {
            var user = profile(people.get(i).email(), allowFreeAuthors);
            var person = result.get(i);
            result.set(
                    i,
                    new VisualAgentRequest.Person(
                            person.name(),
                            user == null ? null : person.photo(),
                            person.autoCrop() && user != null && user.allowsAiPhoto()));
        }
        return List.copyOf(result);
    }

    private User profile(String email, boolean allowFreeAuthors) throws ExecutionException, InterruptedException {
        if (email == null || email.isBlank()) {
            if (allowFreeAuthors) return null;
            throw new BadRequestException("Chaque intervenant doit avoir un profil.");
        }
        try {
            return photos.requireUser(email);
        } catch (NotFoundException missing) {
            if (allowFreeAuthors) return null;
            throw missing;
        }
    }
}
