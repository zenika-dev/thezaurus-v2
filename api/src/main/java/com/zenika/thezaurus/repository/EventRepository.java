package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventRepository {

    @Inject
    Firestore firestore;

    @Inject
    @ConfigProperty(name = "thezaurus.firestore.collection.prefix")
    Optional<String> collectionPrefix;

    private static final String BASE_COLLECTION_NAME = "events";

    private String getCollectionName() {
        if (collectionPrefix.isEmpty() || collectionPrefix.isEmpty() || collectionPrefix.get().trim().isEmpty()) {
            return BASE_COLLECTION_NAME;
        }
        return collectionPrefix.get().trim() + "_" + BASE_COLLECTION_NAME;
    }

    public List<Event> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(getCollectionName()).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Event.class))
                .collect(Collectors.toList());
    }

    public List<Event> findAllByYear(int year) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(getCollectionName())
                .whereGreaterThanOrEqualTo("date", year + "-01-01")
                .whereLessThan("date", (year + 1) + "-01-01")
                .get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Event.class))
                .collect(Collectors.toList());
    }

    public Event findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
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
        DocumentReference docRef = firestore.collection(getCollectionName()).document(event.id());
        ApiFuture<WriteResult> result = docRef.set(event);
        result.get();
        return event;
    }

    public Event update(String id, Event event) throws ExecutionException, InterruptedException {
        Event toSave = event.withId(id);
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<WriteResult> result = docRef.set(toSave);
        result.get();
        return toSave;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult = firestore.collection(getCollectionName()).document(id).delete();
        writeResult.get();
    }
}
