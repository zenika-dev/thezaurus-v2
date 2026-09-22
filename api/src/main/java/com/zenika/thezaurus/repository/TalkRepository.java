package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TemplateContextOption;
import com.zenika.thezaurus.model.TemplateContextPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
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
        return querySnapshot.getDocuments().stream()
                .map(this::toTalkOrNull)
                .filter(Objects::nonNull)
                .toList();
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

    /** Read only preview labels, with a stable document-ID cursor and bounded Firestore reads. */
    public TemplateContextPage findContextOptions(String cursor) throws ExecutionException, InterruptedException {
        int pageSize = 50;
        Query query = firestore
                .collection(getCollectionName())
                .select("title", "date")
                .orderBy(FieldPath.documentId())
                .limit(pageSize + 1);
        if (cursor != null) query = query.startAfter(cursor);
        var documents = query.get().get().getDocuments();
        var options = documents.stream()
                .limit(pageSize)
                .map(document -> {
                    Object title = document.get("title");
                    Object date = document.get("date");
                    String label = title instanceof String value && !value.isBlank() ? value : document.getId();
                    if (date instanceof String value && !value.isBlank()) label += " — " + value;
                    return new TemplateContextOption(document.getId(), label);
                })
                .toList();
        return new TemplateContextPage(
                options,
                documents.size() > pageSize ? documents.get(pageSize - 1).getId() : null);
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
