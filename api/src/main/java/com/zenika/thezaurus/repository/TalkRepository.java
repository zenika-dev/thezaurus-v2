package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.encoding.CustomClassMapper;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.Talk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TalkRepository {

    @Inject
    Firestore firestore;

    @Inject
    Logger logger;

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
        List<Talk> result = new ArrayList<>();
        Map<String, Conference> conferences = new HashMap<>();
        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            Talk talk = toTalkOrNull(document);
            if (talk != null) result.add(resolveConference(talk, conferences));
        }
        return result;
    }

    public Talk findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (!document.exists()) {
            return null;
        }
        return resolveConference(toTalkOrNull(document), new HashMap<>());
    }

    private Talk resolveConference(Talk talk, Map<String, Conference> cache)
            throws ExecutionException, InterruptedException {
        if (talk == null
                || talk.conference() == null
                || talk.conference().getId() == null
                || talk.conference().getId().isBlank()) return talk;
        String id = talk.conference().getId();
        if (!cache.containsKey(id)) {
            String collection = getCollectionName().replaceFirst("talks$", "conferences");
            DocumentSnapshot document =
                    firestore.collection(collection).document(id).get().get();
            cache.put(id, document.exists() ? document.toObject(Conference.class) : null);
        }
        Conference conference = cache.get(id);
        return talk.withConference(
                conference != null
                        ? conference
                        : new Conference(null, talk.conference().getName(), null));
    }

    /**
     * Tente de désérialiser le document en {@link Talk}. Si le document est corrompu ou
     * comporte des données imbriquées invalides, il est ignoré pour éviter de bloquer la liste.
     */
    private Talk toTalkOrNull(DocumentSnapshot doc) {
        try {
            return doc.toObject(Talk.class);
        } catch (RuntimeException e) {
            logger.errorv(e, "Talk {0} illisible, document ignoré", doc.getId());
            return null;
        }
    }

    public Talk create(Talk talk) throws ExecutionException, InterruptedException {
        if (talk.id() == null || talk.id().isEmpty()) {
            talk = talk.withId(UUID.randomUUID().toString());
        }
        return save(talk, null);
    }

    public Talk update(String id, Talk talk, java.util.function.Predicate<Talk> canEdit)
            throws ExecutionException, InterruptedException {
        return save(talk.withId(id), canEdit);
    }

    private Talk save(Talk talk, java.util.function.Predicate<Talk> canEdit)
            throws ExecutionException, InterruptedException {
        DocumentReference reference = firestore.collection(getCollectionName()).document(talk.id());
        return FirestoreTransactions.run(firestore, transaction -> {
            if (canEdit != null) {
                DocumentSnapshot stored = transaction.get(reference).get();
                if (!stored.exists()) throw new WebApplicationException("Talk introuvable", 404);
                if (!canEdit.test(stored.toObject(Talk.class)))
                    throw new WebApplicationException("Modification non autorisée", 403);
            }
            Conference selected = talk.conference();
            Map<String, String> storedConference = null;
            Conference current = selected;
            if (selected != null
                    && selected.getId() != null
                    && !selected.getId().isBlank()) {
                DocumentSnapshot conference = transaction
                        .get(firestore
                                .collection(getCollectionName().replaceFirst("talks$", "conferences"))
                                .document(selected.getId()))
                        .get();
                if (!conference.exists() || Boolean.TRUE.equals(conference.getBoolean("deleting"))) {
                    throw new WebApplicationException(
                            "La conférence n’est plus disponible. Sélectionnez une autre conférence ou saisissez un nom libre.",
                            409);
                }
                current = conference.toObject(Conference.class);
                storedConference = Map.of("id", selected.getId());
            } else if (selected != null
                    && selected.getName() != null
                    && !selected.getName().isBlank()) {
                storedConference = Map.of("name", selected.getName().trim());
                current = new Conference(null, selected.getName().trim(), null);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = new HashMap<>((Map<String, Object>) CustomClassMapper.serialize(talk));
            payload.put("conference", storedConference);
            // Slack historically placed the presentation date inside the embedded conference.
            if ((talk.date() == null || talk.date().isBlank())
                    && selected != null
                    && selected.getId() == null
                    && selected.getDate() != null
                    && selected.getDate().getStart() != null) {
                payload.put("date", selected.getDate().getStart());
            }
            if (canEdit == null) transaction.create(reference, payload);
            else transaction.set(reference, payload);
            return talk.withConference(current).withDate((String) payload.get("date"));
        });
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                firestore.collection(getCollectionName()).document(id).delete();
        writeResult.get();
    }

    /** Réduit les copies embarquées, sans rapprochement par nom ni perte de la date Slack. */
    public int migrateLegacyConferences() throws ExecutionException, InterruptedException {
        int migrated = 0;
        for (DocumentSnapshot document :
                firestore.collection(getCollectionName()).get().get().getDocuments()) {
            if (!(document.get("conference") instanceof Map<?, ?> legacy) || legacy.size() <= 1) continue;
            boolean changed = FirestoreTransactions.run(firestore, transaction -> {
                DocumentSnapshot current =
                        transaction.get(document.getReference()).get();
                if (!current.exists()
                        || !(current.get("conference") instanceof Map<?, ?> embedded)
                        || embedded.size() <= 1) return false;
                Map<String, Object> updates = new HashMap<>();
                Object reference = embedded.get("id");
                Map<String, String> conference = null;
                if (reference instanceof String id && !id.isBlank()) {
                    DocumentSnapshot target = transaction
                            .get(firestore
                                    .collection(getCollectionName().replaceFirst("talks$", "conferences"))
                                    .document(id))
                            .get();
                    if (target.exists() && !Boolean.TRUE.equals(target.getBoolean("deleting")))
                        conference = Map.of("id", id);
                }
                if (conference == null && embedded.get("name") instanceof String name && !name.isBlank()) {
                    conference = Map.of("name", name.trim());
                }
                updates.put("conference", conference);
                String date = current.getString("date");
                if (date == null || date.isBlank()) {
                    Object legacyDate = embedded.get("date");
                    if (legacyDate instanceof Map<?, ?> period && period.get("start") instanceof String start)
                        updates.put("date", start);
                    else if (legacyDate instanceof String start && !start.isBlank()) updates.put("date", start);
                }
                transaction.update(document.getReference(), updates);
                return true;
            });
            if (changed) migrated++;
        }
        return migrated;
    }
}
