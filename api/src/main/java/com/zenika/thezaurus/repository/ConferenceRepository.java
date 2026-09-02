package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.ConferencePeriod;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
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
                .map(doc -> doc.toObject(Conference.class))
                .collect(Collectors.toList());
    }

    public Conference findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(Conference.class);
        }
        return null;
    }

    public Conference create(Conference conference) throws ExecutionException, InterruptedException {
        if (conference.getId() == null || conference.getId().isEmpty()) {
            conference.setId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(getCollectionName()).document(conference.getId());
        ApiFuture<WriteResult> result = docRef.set(conference);
        result.get();
        return conference;
    }

    public Conference update(String id, Conference conference) throws ExecutionException, InterruptedException {
        conference.setId(id);
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<WriteResult> result = docRef.set(conference);
        result.get();
        return conference;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                firestore.collection(getCollectionName()).document(id).delete();
        writeResult.get();
    }

    /**
     * Réécrit les conférences dont le champ {@code date} est encore une chaîne surchargée
     * ({@code "2026-03-01"}, {@code "2026-03-01/2026-03-03"} ou {@code "2026-03"}) vers le format
     * structuré {@link ConferencePeriod}.
     *
     * <p>Idempotente : la détection porte sur le type du champ stocké, donc les documents déjà
     * migrés (une {@code Map}) sont ignorés. Les chaînes non reconnues sont laissées telles quelles
     * et signalées, pour être corrigées à la main plutôt que réécrites arbitrairement.
     *
     * @return le nombre de documents réécrits
     */
    public int migrateLegacyDates() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(getCollectionName()).get().get();
        int migrated = 0;
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            if (!(doc.get("date") instanceof String legacy)) {
                continue;
            }
            ConferencePeriod period = ConferencePeriod.fromLegacyString(legacy);
            if (period == null) {
                logger.warnv(
                        "Conférence {0} : date « {1} » non reconnue, document laissé en l''état", doc.getId(), legacy);
                continue;
            }
            doc.getReference()
                    .update(
                            "date",
                            Map.of(
                                    "start", period.getStart(),
                                    "end", period.getEnd(),
                                    "precision", period.getPrecision().name()))
                    .get();
            migrated++;
        }
        return migrated;
    }
}
