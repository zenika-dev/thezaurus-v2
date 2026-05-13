package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Conference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConferenceRepository {

    @Inject
    Firestore firestore;

    private static final String COLLECTION_NAME = "conferences";

    public List<Conference> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Conference.class))
                .collect(Collectors.toList());
    }

    public Conference findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
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
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(conference.getId());
        ApiFuture<WriteResult> result = docRef.set(conference);
        result.get();
        return conference;
    }

    public Conference update(String id, Conference conference) throws ExecutionException, InterruptedException {
        conference.setId(id);
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> result = docRef.set(conference);
        result.get();
        return conference;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult = firestore.collection(COLLECTION_NAME).document(id).delete();
        writeResult.get();
    }
}
