package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class UserRepository {

    @Inject
    Firestore firestore;

    private static final String COLLECTION_NAME = "users";

    public List<User> findAll(int maxResults) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).limit(maxResults).get();
        return future.get().getDocuments().stream()
                .map(document -> document.toObject(User.class))
                .toList();
    }

    public User findByEmail(String email) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(email);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(User.class);
        }
        return null;
    }

    public User create(User user) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(user.email());
        ApiFuture<WriteResult> result = docRef.set(user);
        result.get();
        return user;
    }

    public User update(String email, User user) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(email);
        ApiFuture<WriteResult> result = docRef.set(user);
        result.get();
        return user;
    }

    /**
     * Réécrit au format courant les utilisateurs qui portent encore l'ancien champ {@code role}
     * mono-valué : le champ est supprimé et, si {@code roles} n'existe pas déjà, il est créé avec
     * l'équivalent normalisé ({@link Role#fromLegacy} : "membre" devient CONSULTANT). Travaille
     * exclusivement sur les données brutes du document — sans passer par le mapping {@link User},
     * qui ne connaît plus ce champ. Idempotent : un document déjà migré n'a plus de champ
     * {@code role}, une seconde exécution retourne donc 0.
     */
    public int migrateLegacyRoles() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME).get().get();
        int migrated = 0;
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            if (!(doc.get("role") instanceof String legacyRole)) {
                continue;
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("role", FieldValue.delete());
            boolean hasRoles = doc.get("roles") instanceof List<?> roles && !roles.isEmpty();
            if (!hasRoles) {
                updates.put("roles", List.of(Role.fromLegacy(legacyRole).name()));
            }
            doc.getReference().update(updates).get();
            migrated++;
        }
        return migrated;
    }
}
