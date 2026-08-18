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
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class UserRepository {

    private static final Logger logger = Logger.getLogger(UserRepository.class);

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
                Role role;
                try {
                    role = Role.fromLegacy(legacyRole);
                } catch (IllegalArgumentException e) {
                    // Une valeur inattendue (champ vidé ou édité à la main dans la console) ne doit
                    // pas empêcher le boot ni bloquer la migration des documents suivants.
                    logger.errorv("Migration rôles legacy : valeur ''{0}'' inconnue sur le document {1}, document ignoré",
                            legacyRole, doc.getId());
                    continue;
                }
                updates.put("roles", List.of(role.name()));
            }
            doc.getReference().update(updates).get();
            migrated++;
        }
        return migrated;
    }

    /**
     * Re-keye en minuscules les documents dont l'id (l'email) contient des majuscules, créés
     * avant la normalisation de l'email du JWT ({@code IapSecurityAugmentor}) : sans cela ils ne
     * seraient plus jamais retrouvés et un doublon serait créé au prochain login. Travaille sur
     * les données brutes des documents. Si le document minuscule existe déjà (vrai doublon), il
     * est prioritaire : seuls ses champs absents ou nuls sont complétés depuis le document
     * majuscule, sauf {@code roles} qui est l'union des deux listes. Idempotent : une seconde
     * exécution ne trouve plus d'id avec majuscule et retourne 0.
     */
    public int migrateLegacyEmailCasing() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(COLLECTION_NAME).get().get();
        int migrated = 0;
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            String id = doc.getId();
            String lowerId = id.toLowerCase(Locale.ROOT);
            if (id.equals(lowerId)) {
                continue;
            }
            DocumentReference target = firestore.collection(COLLECTION_NAME).document(lowerId);
            DocumentSnapshot existing = target.get().get();
            Map<String, Object> data;
            if (existing.exists() && existing.getData() != null) {
                data = mergeUserData(existing.getData(), doc.getData());
            } else {
                data = new HashMap<>(doc.getData());
                data.put("email", data.get("email") instanceof String email
                        ? email.toLowerCase(Locale.ROOT) : lowerId);
            }
            target.set(data).get();
            doc.getReference().delete().get();
            migrated++;
        }
        return migrated;
    }

    /**
     * Fusionne un doublon : les valeurs du document minuscule priment, celles du document
     * majuscule ne comblent que les champs absents ou nuls — sauf {@code roles}, union des deux
     * listes sans doublons (ordre du document minuscule d'abord).
     */
    private static Map<String, Object> mergeUserData(Map<String, Object> lower, Map<String, Object> upper) {
        Map<String, Object> merged = new HashMap<>(lower);
        for (Map.Entry<String, Object> entry : upper.entrySet()) {
            if (!"roles".equals(entry.getKey()) && merged.get(entry.getKey()) == null) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        List<Object> roles = new ArrayList<>();
        if (lower.get("roles") instanceof List<?> lowerRoles) {
            lowerRoles.stream().filter(r -> !roles.contains(r)).forEach(roles::add);
        }
        if (upper.get("roles") instanceof List<?> upperRoles) {
            upperRoles.stream().filter(r -> !roles.contains(r)).forEach(roles::add);
        }
        if (!roles.isEmpty()) {
            merged.put("roles", roles);
        }
        return merged;
    }
}
