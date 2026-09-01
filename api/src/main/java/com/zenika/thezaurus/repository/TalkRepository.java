package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Talk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TalkRepository {

    @Inject
    Firestore firestore;

    @Inject
    @ConfigProperty(name = "thezaurus.firestore.collection.prefix")
    Optional<String> collectionPrefix;

    private static final String BASE_COLLECTION_NAME = "talks";

    private String getCollectionName() {
        if (collectionPrefix == null
                || collectionPrefix.isEmpty()
                || collectionPrefix.get().trim().isEmpty()) {
            return BASE_COLLECTION_NAME;
        }
        return collectionPrefix.get().trim() + "_" + BASE_COLLECTION_NAME;
    }

    public List<Talk> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query =
                firestore.collection(getCollectionName()).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Talk.class))
                .collect(Collectors.toList());
    }

    public Talk findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(Talk.class);
        }
        return null;
    }

    public Talk create(Talk talk) throws ExecutionException, InterruptedException {
        if (talk.id() == null || talk.id().isEmpty()) {
            talk = talk.withId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(getCollectionName()).document(talk.id());
        ApiFuture<WriteResult> result = docRef.set(talk);
        result.get();
        return talk;
    }

    public Talk update(String id, Talk talk) throws ExecutionException, InterruptedException {
        talk = talk.withId(id);
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<WriteResult> result = docRef.set(talk);
        result.get();
        return talk;
    }

    /**
     * Réécrit au format courant les talks dont les speakers sont encore des chaînes (format
     * antérieur à la structuration en User). Travaille exclusivement sur les données brutes du
     * document — sans passer par le mapping {@link Talk}, qui ne sait plus lire l'ancien format —
     * et ne touche qu'au champ {@code speakers}. Idempotent : un document déjà migré n'est ni
     * réécrit ni compté, une seconde exécution retourne donc 0.
     */
    public int migrateLegacySpeakers() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(getCollectionName()).get().get();
        int migrated = 0;
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            Object rawSpeakers = doc.get("speakers");
            boolean legacy =
                    rawSpeakers instanceof List<?> elements && elements.stream().anyMatch(e -> e instanceof String);
            if (!legacy) {
                continue;
            }
            List<Object> converted = ((List<?>) rawSpeakers)
                    .stream()
                            .map(e -> e instanceof String name ? Map.of("name", name) : e)
                            .collect(Collectors.toList());
            doc.getReference().update("speakers", converted).get();
            migrated++;
        }
        return migrated;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                firestore.collection(getCollectionName()).document(id).delete();
        writeResult.get();
    }
}
