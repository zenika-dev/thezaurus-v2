package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.ConferencePeriod;
import com.zenika.thezaurus.model.DatePrecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
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
        List<Conference> conferences = new ArrayList<>();
        for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
            Conference conference = toConferenceOrNull(doc);
            if (conference != null) {
                conferences.add(conference);
            }
        }
        return conferences;
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
     * {@code toObject} lève si {@code date} n'est pas au format {@link ConferencePeriod} attendu —
     * document non migré (staging, export ancien, restauration) ou corrompu. Un seul document dans
     * cet état ne doit pas faire échouer la liste entière pour tout le monde.
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
     * Réécrit les conférences dont {@code date} est encore une chaîne surchargée vers le format
     * structuré {@link ConferencePeriod}. Idempotente : la détection porte sur le type du champ
     * stocké, donc les documents déjà migrés ({@code Map}) sont ignorés.
     *
     * <p>Une chaîne non reconnue est réécrite en période vide plutôt que laissée telle quelle :
     * {@code Conference.date} est typé {@link ConferencePeriod}, donc une {@code String} restante
     * ferait échouer la désérialisation Firestore de ce document — et de tout appel qui liste la
     * collection — à la première lecture. Une période vide reste un {@code Map} valide ; le
     * document est signalé pour correction manuelle sans jamais faire planter une lecture.
     *
     * <p>Les écritures sont groupées par {@link WriteBatch} de {@link #BATCH_SIZE} plutôt
     * qu'envoyées une par une : au démarrage, un aller-retour Firestore par document risquerait
     * de dépasser les délais des sondes de liveness/readiness sur une collection volumineuse.
     *
     * @return le nombre de documents réécrits
     */
    public int migrateLegacyDates() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection(getCollectionName()).get().get();
        int migrated = 0;
        WriteBatch batch = firestore.batch();
        int pending = 0;
        for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
            if (!(doc.get("date") instanceof String legacy)) {
                continue;
            }
            ConferencePeriod period = ConferencePeriod.fromLegacyString(legacy);
            if (period == null) {
                logger.warnv(
                        "Conférence {0} : date « {1} » non reconnue, réécrite en période vide — à corriger manuellement",
                        doc.getId(), legacy);
                period = new ConferencePeriod("", "", DatePrecision.DAY);
            }
            batch.update(doc.getReference(), "date", period.toFirestoreMap());
            migrated++;
            if (++pending == BATCH_SIZE) {
                batch.commit().get();
                batch = firestore.batch();
                pending = 0;
            }
        }
        if (pending > 0) {
            batch.commit().get();
        }
        return migrated;
    }
}
