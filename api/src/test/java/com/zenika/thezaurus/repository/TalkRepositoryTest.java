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
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.Talk;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TalkRepositoryTest {

    private TalkRepository repository;
    private CollectionReference collection;
    private QuerySnapshot snapshot;

    @Test
    public void selectedConferenceAlwaysUsesCurrentCatalogueInformation() throws Exception {
        DocumentReference talkRef = Mockito.mock(DocumentReference.class);
        DocumentSnapshot talkDoc = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(collection.document("talk")).thenReturn(talkRef);
        Mockito.when(talkRef.get()).thenReturn(ApiFutures.immediateFuture(talkDoc));
        Mockito.when(talkDoc.exists()).thenReturn(true);
        Mockito.when(talkDoc.toObject(Talk.class))
                .thenReturn(
                        new Talk("talk", "Title", "Abstract").withConference(new Conference("conf", "Old name", null)));
        CollectionReference conferences = Mockito.mock(CollectionReference.class);
        DocumentReference conferenceRef = Mockito.mock(DocumentReference.class);
        DocumentSnapshot conferenceDoc = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(repository.firestore.collection("conferences")).thenReturn(conferences);
        Mockito.when(conferences.document("conf")).thenReturn(conferenceRef);
        Mockito.when(conferenceRef.get()).thenReturn(ApiFutures.immediateFuture(conferenceDoc));
        Mockito.when(conferenceDoc.exists()).thenReturn(true);
        Mockito.when(conferenceDoc.toObject(Conference.class)).thenReturn(new Conference("conf", "Current name", null));
        assertEquals("Current name", repository.findById("talk").conference().getName());
    }

    @Test
    public void creatingATalkStoresOnlyTheSelectedConferenceId() throws Exception {
        DocumentReference talkRef = Mockito.mock(DocumentReference.class);
        Mockito.when(collection.document("talk")).thenReturn(talkRef);
        com.google.cloud.firestore.Transaction transaction = Mockito.mock(com.google.cloud.firestore.Transaction.class);
        CollectionReference conferences = Mockito.mock(CollectionReference.class);
        DocumentReference conferenceRef = Mockito.mock(DocumentReference.class);
        DocumentSnapshot conferenceDoc = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(repository.firestore.collection("conferences")).thenReturn(conferences);
        Mockito.when(conferences.document("conf")).thenReturn(conferenceRef);
        Mockito.when(transaction.get(conferenceRef)).thenReturn(ApiFutures.immediateFuture(conferenceDoc));
        Mockito.when(conferenceDoc.exists()).thenReturn(true);
        Mockito.when(conferenceDoc.toObject(Conference.class)).thenReturn(new Conference("conf", "Current name", null));
        Mockito.when(repository.firestore.runTransaction(
                        Mockito.any(com.google.cloud.firestore.Transaction.Function.class)))
                .thenAnswer(invocation -> ApiFutures.immediateFuture(
                        ((com.google.cloud.firestore.Transaction.Function<?>) invocation.getArgument(0))
                                .updateCallback(transaction)));
        // The legacy implementation writes the whole object directly.
        Mockito.when(talkRef.set(Mockito.any(Object.class))).thenReturn(ApiFutures.immediateFuture(null));
        repository.create(
                new Talk("talk", "Title", "Abstract").withConference(new Conference("conf", "Old name", null)));
        org.mockito.ArgumentCaptor<java.util.Map> payload = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        Mockito.verify(transaction).create(Mockito.eq(talkRef), payload.capture());
        assertEquals(java.util.Map.of("id", "conf"), ((java.util.Map<?, ?>) payload.getValue()).get("conference"));
    }

    @Test
    public void migrationKeepsAnUnknownConferenceFreeAndPreservesTheSlackDate() throws Exception {
        QueryDocumentSnapshot legacy = Mockito.mock(QueryDocumentSnapshot.class);
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        Mockito.when(legacy.getReference()).thenReturn(reference);
        Mockito.when(legacy.exists()).thenReturn(true);
        Mockito.when(legacy.get("conference"))
                .thenReturn(
                        java.util.Map.of("name", "Community meetup", "date", java.util.Map.of("start", "2026-09-23")));
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(legacy));
        com.google.cloud.firestore.Transaction transaction = Mockito.mock(com.google.cloud.firestore.Transaction.class);
        Mockito.when(transaction.get(reference)).thenReturn(ApiFutures.immediateFuture(legacy));
        Mockito.when(repository.firestore.runTransaction(
                        Mockito.any(com.google.cloud.firestore.Transaction.Function.class)))
                .thenAnswer(invocation -> ApiFutures.immediateFuture(
                        ((com.google.cloud.firestore.Transaction.Function<?>) invocation.getArgument(0))
                                .updateCallback(transaction)));
        assertEquals(1, repository.migrateLegacyConferences());
        org.mockito.ArgumentCaptor<java.util.Map> updates = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        Mockito.verify(transaction).update(Mockito.eq(reference), updates.capture());
        assertEquals(
                java.util.Map.of("name", "Community meetup"), updates.getValue().get("conference"));
        assertEquals("2026-09-23", updates.getValue().get("date"));
    }

    @Test
    public void migrationIgnoresAlreadyMigratedConferenceReferences() throws Exception {
        QueryDocumentSnapshot migrated = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(migrated.get("conference")).thenReturn(java.util.Map.of("id", "conf"));
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(migrated));
        assertEquals(0, repository.migrateLegacyConferences());
    }

    @BeforeEach
    public void setUp() {
        Firestore firestore = Mockito.mock(Firestore.class);
        collection = Mockito.mock(CollectionReference.class);
        snapshot = Mockito.mock(QuerySnapshot.class);

        Mockito.when(firestore.collection("talks")).thenReturn(collection);
        Mockito.when(collection.get()).thenReturn(ApiFutures.immediateFuture(snapshot));

        repository = new TalkRepository();
        repository.firestore = firestore;
        repository.logger = Mockito.mock(Logger.class);
        repository.collectionPrefix = Optional.empty();
    }

    // --- Lecture défensive : un document illisible n'interrompt pas la liste --------------------
    // Une conférence embarquée dans un talk peut comporter des données mal formées : la
    // désérialisation du talk est ignorée sans faire échouer la lecture des autres éléments.

    @Test
    public void findAllSkipsADocumentThatFailsToDeserializeInsteadOfFailingTheWholeList() throws Exception {
        QueryDocumentSnapshot broken = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(broken.getId()).thenReturn("broken-id");
        Mockito.when(broken.toObject(Talk.class)).thenThrow(new RuntimeException("format inattendu"));

        Talk valid = new Talk("ok-id", "Un talk", "Description");
        QueryDocumentSnapshot okDoc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(okDoc.toObject(Talk.class)).thenReturn(valid);

        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(broken, okDoc));

        List<Talk> result = repository.findAll();

        assertEquals(List.of(valid), result);
    }

    @Test
    public void findAllOnAllDocumentsBrokenReturnsAnEmptyListRatherThanThrowing() throws Exception {
        QueryDocumentSnapshot broken = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(broken.getId()).thenReturn("broken-id");
        Mockito.when(broken.toObject(Talk.class)).thenThrow(new RuntimeException("format inattendu"));
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(broken));

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    public void findByIdReturnsNullRatherThanThrowingOnADeserializationFailure() throws Exception {
        DocumentReference docRef = Mockito.mock(DocumentReference.class);
        DocumentSnapshot document = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(document.exists()).thenReturn(true);
        Mockito.when(document.getId()).thenReturn("broken-id");
        Mockito.when(document.toObject(Talk.class)).thenThrow(new RuntimeException("format inattendu"));
        Mockito.when(docRef.get()).thenReturn(ApiFutures.immediateFuture(document));
        Mockito.when(collection.document("broken-id")).thenReturn(docRef);

        assertNull(repository.findById("broken-id"));
    }
}
