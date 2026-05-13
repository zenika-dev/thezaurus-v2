package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Talk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class TalkRepository {

    @Inject
    Firestore firestore;

    private static final String COLLECTION_NAME = "talks";

    public List<Talk> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Talk.class))
                .collect(Collectors.toList());
    }

    public Talk findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(Talk.class);
        }
        return null;
    }

    public Talk create(Talk talk) throws ExecutionException, InterruptedException {
        if (talk.getId() == null || talk.getId().isEmpty()) {
            talk.setId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(talk.getId());
        ApiFuture<WriteResult> result = docRef.set(talk);
        result.get();
        return talk;
    }

    public Talk update(String id, Talk talk) throws ExecutionException, InterruptedException {
        talk.setId(id);
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> result = docRef.set(talk);
        result.get();
        return talk;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult = firestore.collection(COLLECTION_NAME).document(id).delete();
        writeResult.get();
    }
}
