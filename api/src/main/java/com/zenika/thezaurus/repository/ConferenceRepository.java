package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Conference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ConferenceRepository {

    @Inject
    Firestore firestore;

    @Inject
    Logger logger;

    @Inject
    @ConfigProperty(name = "thezaurus.firestore.collection.prefix")
    Optional<String> collectionPrefix;

    private static final String BASE_COLLECTION_NAME = "conferences";

    /** Limite Firestore : un WriteBatch accepte au plus 500 opérations. */
    private static final int BATCH_SIZE = 500;

    private String getCollectionName() {
        if (collectionPrefix == null
                || collectionPrefix.isEmpty()
                || collectionPrefix.get().trim().isEmpty()) {
            return BASE_COLLECTION_NAME;
        }
        return collectionPrefix.get().trim() + "_" + BASE_COLLECTION_NAME;
    }

    public List<Conference> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query =
                firestore.collection(getCollectionName()).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(this::toConferenceOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    public Conference findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (!document.exists()) {
            return null;
        }
        return toConferenceOrNull(document);
    }

    /**
     * Tente de désérialiser le document en {@link Conference}. Si le document est corrompu ou
     * comporte un format de date inattendu, il est ignoré pour ne pas faire échouer la liste.
     */
    private Conference toConferenceOrNull(DocumentSnapshot doc) {
        try {
            return doc.toObject(Conference.class);
        } catch (RuntimeException e) {
            logger.errorv(e, "Conférence {0} illisible, document ignoré", doc.getId());
            return null;
        }
    }

    public Conference create(Conference conference) throws ExecutionException, InterruptedException {
        if (conference.getId() == null || conference.getId().isEmpty()) {
            conference.setId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(getCollectionName()).document(conference.getId());
        ApiFuture<WriteResult> result = docRef.create(conference);
        result.get();
        return conference;
    }

    public Conference update(String id, Conference conference) throws ExecutionException, InterruptedException {
        conference.setId(id);
        DocumentReference reference = firestore.collection(getCollectionName()).document(id);
        return FirestoreTransactions.run(firestore, transaction -> {
            DocumentSnapshot existing = transaction.get(reference).get();
            if (!existing.exists()) throw new WebApplicationException("Conférence introuvable", 404);
            if (Boolean.TRUE.equals(existing.getBoolean("deleting"))) {
                throw new WebApplicationException("Suppression de la conférence en cours", 409);
            }
            transaction.set(reference, conference);
            return conference;
        });
    }

    /**
     * Marque d'abord la conférence pour refuser de nouveaux rattachements. Les conversions
     * se font par transactions de 400 talks, sans écraser leurs autres champs. Une interruption
     * conserve la conférence et sa marque ; relancer la suppression reprend le travail restant.
     */
    public void delete(String id) throws ExecutionException, InterruptedException {
        DocumentReference reference = firestore.collection(getCollectionName()).document(id);
        FirestoreTransactions.run(firestore, transaction -> {
            if (transaction.get(reference).get().exists()) transaction.update(reference, "deleting", true);
            return null;
        });
        boolean finished;
        do {
            finished = FirestoreTransactions.run(firestore, transaction -> {
                DocumentSnapshot conference = transaction.get(reference).get();
                if (!conference.exists()) return true;
                QuerySnapshot talks = transaction
                        .get(firestore
                                .collection(getCollectionName().replaceFirst("conferences$", "talks"))
                                .whereEqualTo("conference.id", id)
                                .limit(400))
                        .get();
                if (talks.getDocuments().isEmpty()) {
                    transaction.delete(reference);
                    return true;
                }
                String name = conference.getString("name");
                for (QueryDocumentSnapshot talk : talks.getDocuments()) {
                    transaction.update(talk.getReference(), "conference", Map.of("name", name));
                }
                return false;
            });
        } while (!finished);
    }
}
