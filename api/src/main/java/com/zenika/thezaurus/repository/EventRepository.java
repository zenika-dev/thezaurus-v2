package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Event;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EventRepository {

    @Inject
    Firestore firestore;

    @Inject
    @ConfigProperty(name = "thezaurus.firestore.collection.prefix")
    Optional<String> collectionPrefix;

    private static final String BASE_COLLECTION_NAME = "events";

    private String collectionName;

    @PostConstruct
    void init() {
        collectionName = Optional.ofNullable(collectionPrefix)
                .flatMap(prefix -> prefix)
                .map(String::trim)
                .filter(prefix -> !prefix.isEmpty())
                .map(prefix -> prefix + "_" + BASE_COLLECTION_NAME)
                .orElse(BASE_COLLECTION_NAME);
    }

    public List<Event> findAll() throws ExecutionException, InterruptedException {
        return findAll(null, null);
    }

    public List<Event> findAll(Integer page, Integer size) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(collectionName);
        if (size != null) {
            int resolvedPage = page != null ? Math.max(page, 0) : 0;
            query = query.offset(resolvedPage * size).limit(size);
        }
        QuerySnapshot querySnapshot = query.get().get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Event.class))
                .collect(Collectors.toList());
    }

    public List<Event> findAllByYear(int year) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore
                .collection(collectionName)
                .whereGreaterThanOrEqualTo("date", year + "-01-01")
                .whereLessThan("date", (year + 1) + "-01-01")
                .get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Event.class))
                .collect(Collectors.toList());
    }

    public Event findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(Event.class);
        }
        return null;
    }

    public Event create(Event event) throws ExecutionException, InterruptedException {
        if (event.id() == null || event.id().isEmpty()) {
            event = event.withId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(collectionName).document(event.id());
        ApiFuture<WriteResult> result = docRef.set(event);
        result.get();
        return event;
    }

    public Event update(String id, Event event) throws ExecutionException, InterruptedException {
        Event toSave = event.withId(id);
        DocumentReference docRef = firestore.collection(collectionName).document(id);
        ApiFuture<WriteResult> result = docRef.set(toSave);
        result.get();
        return toSave;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                firestore.collection(collectionName).document(id).delete();
        writeResult.get();
    }
}
