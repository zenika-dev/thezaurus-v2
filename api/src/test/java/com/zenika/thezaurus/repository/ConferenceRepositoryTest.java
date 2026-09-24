package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Conference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ConferenceRepositoryTest {

    private ConferenceRepository repository;
    private CollectionReference collection;
    private QuerySnapshot snapshot;
    private WriteBatch batch;

    @BeforeEach
    public void setUp() {
        Firestore firestore = Mockito.mock(Firestore.class);
        collection = Mockito.mock(CollectionReference.class);
        snapshot = Mockito.mock(QuerySnapshot.class);
        batch = Mockito.mock(WriteBatch.class);

        Mockito.when(firestore.collection("conferences")).thenReturn(collection);
        Mockito.when(collection.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        Mockito.when(firestore.batch()).thenReturn(batch);
        Mockito.when(batch.update(Mockito.any(DocumentReference.class), Mockito.anyString(), Mockito.any()))
                .thenReturn(batch);
        Mockito.when(batch.commit()).thenReturn(ApiFutures.immediateFuture(List.of(Mockito.mock(WriteResult.class))));

        repository = new ConferenceRepository();
        repository.firestore = firestore;
        repository.logger = Mockito.mock(Logger.class);
        repository.collectionPrefix = Optional.empty();
    }

    @Test
    public void deletingConferenceConvertsAssociatedTalksToFreeNames() throws Exception {
        DocumentReference conferenceRef = Mockito.mock(DocumentReference.class);
        DocumentSnapshot conferenceDoc = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(collection.document("conf")).thenReturn(conferenceRef);
        Mockito.when(conferenceDoc.exists()).thenReturn(true);
        Mockito.when(conferenceDoc.getString("name")).thenReturn("Devoxx 2026");
        Mockito.when(conferenceRef.delete()).thenReturn(ApiFutures.immediateFuture(null));
        com.google.cloud.firestore.Transaction transaction = Mockito.mock(com.google.cloud.firestore.Transaction.class);
        Mockito.when(transaction.get(conferenceRef)).thenReturn(ApiFutures.immediateFuture(conferenceDoc));
        CollectionReference talks = Mockito.mock(CollectionReference.class);
        com.google.cloud.firestore.Query query = Mockito.mock(com.google.cloud.firestore.Query.class);
        Mockito.when(repository.firestore.collection("talks")).thenReturn(talks);
        Mockito.when(talks.whereEqualTo("conference.id", "conf")).thenReturn(query);
        Mockito.when(query.limit(400)).thenReturn(query);
        QuerySnapshot associated = Mockito.mock(QuerySnapshot.class);
        QuerySnapshot empty = Mockito.mock(QuerySnapshot.class);
        DocumentReference talkRef = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot talk = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(talk.getReference()).thenReturn(talkRef);
        Mockito.when(associated.getDocuments()).thenReturn(List.of(talk));
        Mockito.when(empty.getDocuments()).thenReturn(List.of());
        Mockito.when(transaction.get(query))
                .thenReturn(ApiFutures.immediateFuture(associated), ApiFutures.immediateFuture(empty));
        Mockito.when(repository.firestore.runTransaction(
                        Mockito.any(com.google.cloud.firestore.Transaction.Function.class)))
                .thenAnswer(invocation -> ApiFutures.immediateFuture(
                        ((com.google.cloud.firestore.Transaction.Function<?>) invocation.getArgument(0))
                                .updateCallback(transaction)));
        repository.delete("conf");
        Mockito.verify(transaction).update(talkRef, "conference", Map.of("name", "Devoxx 2026"));
        Mockito.verify(transaction).delete(conferenceRef);
    }

    // --- Lecture defensive : un document illisible ne fait pas echouer les autres ---------------

    @Test
    public void findAllSkipsADocumentThatFailsToDeserializeInsteadOfFailingTheWholeList() throws Exception {
        // Cas d'un document portant un format de date inattendu ou corrompu faisant lever toObject().
        QueryDocumentSnapshot broken = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(broken.getId()).thenReturn("broken-id");
        Mockito.when(broken.toObject(Conference.class)).thenThrow(new RuntimeException("format inattendu"));

        Conference valid = new Conference("ok-id", "Devoxx", null);
        QueryDocumentSnapshot okDoc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(okDoc.toObject(Conference.class)).thenReturn(valid);

        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(broken, okDoc));

        List<Conference> result = repository.findAll();

        assertEquals(List.of(valid), result);
    }

    @Test
    public void findAllOnAllDocumentsBrokenReturnsAnEmptyListRatherThanThrowing() throws Exception {
        QueryDocumentSnapshot broken = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(broken.getId()).thenReturn("broken-id");
        Mockito.when(broken.toObject(Conference.class)).thenThrow(new RuntimeException("format inattendu"));
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(broken));

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    public void findByIdReturnsNullRatherThanThrowingOnADeserializationFailure() throws Exception {
        DocumentReference docRef = Mockito.mock(DocumentReference.class);
        DocumentSnapshot document = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(document.exists()).thenReturn(true);
        Mockito.when(document.getId()).thenReturn("broken-id");
        Mockito.when(document.toObject(Conference.class)).thenThrow(new RuntimeException("format inattendu"));
        Mockito.when(docRef.get()).thenReturn(ApiFutures.immediateFuture(document));
        Mockito.when(collection.document("broken-id")).thenReturn(docRef);

        assertNull(repository.findById("broken-id"));
    }

    private QueryDocumentSnapshot documentOf(Object rawDate, DocumentReference reference) {
        QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(doc.get("date")).thenReturn(rawDate);
        Mockito.when(doc.getReference()).thenReturn(reference);
        Mockito.when(doc.getId()).thenReturn("doc-id");
        return doc;
    }
}
