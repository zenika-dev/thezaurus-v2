package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.util.SearchSanitizer;
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
        ApiFuture<QuerySnapshot> future =
                firestore.collection(COLLECTION_NAME).limit(maxResults).get();
        return future.get().getDocuments().stream()
                .map(document -> document.toObject(User.class))
                .toList();
    }

    public List<User> search(String query, int limit) throws ExecutionException, InterruptedException {
        int boundedLimit = Math.clamp(limit, 1, 100);
        String normalizedQuery = SearchSanitizer.normalizeForSearch(query);
        if (normalizedQuery.isBlank()) {
            ApiFuture<QuerySnapshot> future =
                    firestore.collection(COLLECTION_NAME).limit(boundedLimit).get();
            return future.get().getDocuments().stream()
                    .map(document -> document.toObject(User.class))
                    .toList();
        }

        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        return future.get().getDocuments().stream()
                .map(document -> document.toObject(User.class))
                .filter(user -> matchesSearch(user, normalizedQuery))
                .limit(boundedLimit)
                .toList();
    }

    static boolean matchesSearch(User user, String normalizedQuery) {
        if (user == null) {
            return false;
        }
        return SearchSanitizer.containsNormalized(user.name(), normalizedQuery)
                || SearchSanitizer.containsNormalized(user.email(), normalizedQuery);
    }

    public List<User> search(String query) throws ExecutionException, InterruptedException {
        return search(query, 20);
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
     * Écriture ciblée : {@link #update(String, User)} ferait un {@code set} qui écrase le document
     * entier, donc les rôles et le {@code slackUserId}. Échoue si le document n'existe pas.
     */
    public void updateNotificationPreferences(String email, boolean emailEnabled, boolean slackEnabled)
            throws ExecutionException, InterruptedException {
        Map<String, Object> updates = new HashMap<>();
        updates.put("emailNotificationsEnabled", emailEnabled);
        updates.put("slackNotificationsEnabled", slackEnabled);
        firestore.collection(COLLECTION_NAME).document(email).update(updates).get();
    }

    /** Écriture ciblée, cf. {@link #updateNotificationPreferences}. */
    public void updateSlackUserId(String email, String slackUserId) throws ExecutionException, InterruptedException {
        firestore
                .collection(COLLECTION_NAME)
                .document(email)
                .update(Map.of("slackUserId", slackUserId))
                .get();
    }

    /** Écriture ciblée, cf. {@link #updateNotificationPreferences}. */
    public void updateName(String email, String name) throws ExecutionException, InterruptedException {
        firestore
                .collection(COLLECTION_NAME)
                .document(email)
                .update(Map.of("name", name))
                .get();
    }
}
