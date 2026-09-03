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
import java.util.ArrayList;
import java.util.List;
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
        List<Talk> talks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
            Talk talk = toTalkOrNull(doc);
            if (talk != null) {
                talks.add(talk);
            }
        }
        return talks;
    }

    public Talk findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (!document.exists()) {
            return null;
        }
        return toTalkOrNull(document);
    }

    /**
     * {@code toObject} lève si la {@code Conference} imbriquée porte un {@code date} au format
     * legacy (chaîne, avant {@code ConferencePeriod}) : cas des talks créés via la commande Slack
     * avant cette migration, qui n'a aucune prise sur les conférences embarquées dans un talk. Un
     * seul document dans cet état ne doit pas faire échouer la liste entière pour tout le monde.
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

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                firestore.collection(getCollectionName()).document(id).delete();
        writeResult.get();
    }
}
