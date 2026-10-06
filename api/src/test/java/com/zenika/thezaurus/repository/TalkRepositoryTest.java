package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

public class TalkRepositoryTest {

    @Test
    void markingReminderSentOnlyUpdatesTheMarker() throws Exception {
        DocumentReference document = Mockito.mock(DocumentReference.class);
        Mockito.when(collection.document("talk-1")).thenReturn(document);
        Mockito.when(document.update("feedbackReminderSent", true)).thenReturn(ApiFutures.immediateFuture(null));

        repository.markFeedbackReminderSent("talk-1");

        Mockito.verify(document).update("feedbackReminderSent", true);
        Mockito.verify(document, Mockito.never()).set(Mockito.any(Talk.class));
    }

    @Test
    void creatingATalkIgnoresTheClientReminderMarker() throws Exception {
        DocumentReference document = Mockito.mock(DocumentReference.class);
        Mockito.when(collection.document("talk-1")).thenReturn(document);
        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(repository.firestore.runTransaction(Mockito.any(Transaction.Function.class)))
                .thenAnswer(invocation -> {
                    Transaction.Function<?> callback = invocation.getArgument(0);
                    return ApiFutures.immediateFuture(callback.updateCallback(transaction));
                });
        Talk requested = new Talk(
                "talk-1",
                "Talk",
                "Description",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true);

        Talk created = repository.create(requested);

        assertEquals(false, created.feedbackReminderSent());
        Mockito.verify(transaction).create(Mockito.eq(document), Mockito.any(Object.class));
    }

    @Test
    void creatingAnExistingTalkNeverOverwritesItsReminderMarker() {
        DocumentReference document = Mockito.mock(DocumentReference.class);
        Mockito.when(collection.document("talk-1")).thenReturn(document);
        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(transaction.create(Mockito.any(), Mockito.any()))
                .thenThrow(new IllegalStateException("Document already exists"));
        Mockito.when(repository.firestore.runTransaction(Mockito.any(Transaction.Function.class)))
                .thenAnswer(invocation -> {
                    Transaction.Function<?> callback = invocation.getArgument(0);
                    return ApiFutures.immediateFuture(callback.updateCallback(transaction));
                });

        assertThrows(ExecutionException.class, () -> repository.create(new Talk("talk-1", "Talk", "Description")));

        Mockito.verify(document, Mockito.never()).set(Mockito.any(Talk.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void editingATalkPreservesTheStoredReminderMarker(Boolean stored) throws Exception {
        DocumentReference document = Mockito.mock(DocumentReference.class);
        DocumentSnapshot current = Mockito.mock(DocumentSnapshot.class);
        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(collection.document("talk-1")).thenReturn(document);
        Mockito.when(current.exists()).thenReturn(true);
        Mockito.when(current.getBoolean("feedbackReminderSent")).thenReturn(stored);
        Mockito.when(current.toObject(Talk.class)).thenReturn(new Talk("talk-1", "Old talk", "Description"));
        Mockito.when(transaction.get(document)).thenReturn(ApiFutures.immediateFuture(current));
        Mockito.when(repository.firestore.runTransaction(Mockito.any(Transaction.Function.class)))
                .thenAnswer(invocation -> {
                    Transaction.Function<?> callback = invocation.getArgument(0);
                    return ApiFutures.immediateFuture(callback.updateCallback(transaction));
                });
        Talk requested = new Talk("ignored-id", "Edited talk", "Description")
                .withFeedbackReminderSent(!Boolean.TRUE.equals(stored));

        Talk updated = repository.update("talk-1", requested, talk -> true);

        assertEquals(Boolean.TRUE.equals(stored), updated.feedbackReminderSent());
        assertEquals("talk-1", updated.id());
        assertEquals("Edited talk", updated.title());
        var ordered = Mockito.inOrder(transaction);
        ordered.verify(transaction).get(document);
        ordered.verify(transaction).set(Mockito.eq(document), Mockito.any(Object.class));
        Mockito.verify(document, Mockito.never()).set(Mockito.any(Talk.class));
    }

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
    @Test
    public void contextPageUsesProjectionLimitAndStableCursor() throws Exception {
        var query = Mockito.mock(com.google.cloud.firestore.Query.class, Mockito.RETURNS_SELF);
        Mockito.when(collection.select("title", "date")).thenReturn(query);
        Mockito.when(query.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        var documents = java.util.stream.IntStream.range(0, 51)
                .mapToObj(index -> {
                    var document = Mockito.mock(QueryDocumentSnapshot.class);
                    Mockito.when(document.getId()).thenReturn("id-" + index);
                    Mockito.when(document.get("title")).thenReturn("Talk " + index);
                    Mockito.when(document.get("date")).thenReturn("2026-09-22");
                    return document;
                })
                .toList();
        Mockito.when(snapshot.getDocuments()).thenReturn(documents);
        var page = repository.findContextOptions("previous");
        assertEquals(50, page.options().size());
        assertEquals("id-49", page.nextCursor());
        assertEquals("Talk 0 — 2026-09-22", page.options().getFirst().label());
        Mockito.verify(query).orderBy(com.google.cloud.firestore.FieldPath.documentId());
        Mockito.verify(query).startAfter("previous");
        Mockito.verify(query).limit(51);
        Mockito.verify(collection, Mockito.never()).get();
        Mockito.when(snapshot.getDocuments()).thenReturn(documents.subList(0, 1));
        assertNull(repository.findContextOptions(null).nextCursor());
    }

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

    @Test
    public void findFeedbackReminderCandidatesQueriesOnlyUnsentAcceptedOrDoneTalks() throws Exception {
        var query = Mockito.mock(com.google.cloud.firestore.Query.class);
        var statusQuery = Mockito.mock(com.google.cloud.firestore.Query.class);
        var dateQuery = Mockito.mock(com.google.cloud.firestore.Query.class);
        LocalDate today = LocalDate.of(2026, 10, 5);
        Mockito.when(collection.whereEqualTo("feedbackReminderSent", false)).thenReturn(query);
        Mockito.when(query.whereIn("status", List.of(TalkStatus.ACCEPTED.name(), TalkStatus.DONE.name())))
                .thenReturn(statusQuery);
        Mockito.when(statusQuery.whereLessThan("date", "2026-10-05")).thenReturn(dateQuery);
        Mockito.when(dateQuery.get()).thenReturn(ApiFutures.immediateFuture(snapshot));

        Talk valid = new Talk("cand-1", "Titre", "Description");
        QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(doc.toObject(Talk.class)).thenReturn(valid);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        List<Talk> candidates = repository.findFeedbackReminderCandidates(today);

        assertEquals(List.of(valid), candidates);
        Mockito.verify(collection).whereEqualTo("feedbackReminderSent", false);
        Mockito.verify(query).whereIn("status", List.of(TalkStatus.ACCEPTED.name(), TalkStatus.DONE.name()));
        Mockito.verify(statusQuery).whereLessThan("date", "2026-10-05");
    }

    @Test
    public void migrateFeedbackReminderSentSetsFalseOnDocumentsWithoutTheMarker() throws Exception {
        WriteBatch batch = Mockito.mock(WriteBatch.class);
        Mockito.when(repository.firestore.batch()).thenReturn(batch);
        Mockito.when(batch.commit()).thenReturn(ApiFutures.immediateFuture(List.of(Mockito.mock(WriteResult.class))));

        DocumentReference refMissing = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot docMissing = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(docMissing.contains("feedbackReminderSent")).thenReturn(false);
        Mockito.when(docMissing.getReference()).thenReturn(refMissing);

        DocumentReference refNull = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot docNull = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(docNull.contains("feedbackReminderSent")).thenReturn(true);
        Mockito.when(docNull.get("feedbackReminderSent")).thenReturn(null);
        Mockito.when(docNull.getReference()).thenReturn(refNull);

        QueryDocumentSnapshot docFalse = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(docFalse.contains("feedbackReminderSent")).thenReturn(true);
        Mockito.when(docFalse.get("feedbackReminderSent")).thenReturn(false);

        QueryDocumentSnapshot docTrue = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(docTrue.contains("feedbackReminderSent")).thenReturn(true);
        Mockito.when(docTrue.get("feedbackReminderSent")).thenReturn(true);

        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(docMissing, docNull, docFalse, docTrue));

        int migrated = repository.migrateFeedbackReminderSent();

        assertEquals(2, migrated);
        Mockito.verify(batch).update(refMissing, "feedbackReminderSent", false);
        Mockito.verify(batch).update(refNull, "feedbackReminderSent", false);
        Mockito.verify(batch).commit();
    }

    @Test
    public void migrateFeedbackReminderSentCommitsInBatchesOf500() throws Exception {
        WriteBatch batch1 = Mockito.mock(WriteBatch.class);
        WriteBatch batch2 = Mockito.mock(WriteBatch.class);
        Mockito.when(repository.firestore.batch()).thenReturn(batch1, batch2);
        Mockito.when(batch1.commit()).thenReturn(ApiFutures.immediateFuture(List.of(Mockito.mock(WriteResult.class))));
        Mockito.when(batch2.commit()).thenReturn(ApiFutures.immediateFuture(List.of(Mockito.mock(WriteResult.class))));

        List<QueryDocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);
            DocumentReference ref = Mockito.mock(DocumentReference.class);
            Mockito.when(doc.contains("feedbackReminderSent")).thenReturn(false);
            Mockito.when(doc.getReference()).thenReturn(ref);
            docs.add(doc);
        }
        Mockito.when(snapshot.getDocuments()).thenReturn(docs);

        int migrated = repository.migrateFeedbackReminderSent();

        assertEquals(501, migrated);
        Mockito.verify(batch1).commit();
        Mockito.verify(batch2).commit();
    }

    @Test
    public void migrateFeedbackReminderSentOnEmptyCollection() throws Exception {
        WriteBatch batch = Mockito.mock(WriteBatch.class);
        Mockito.when(repository.firestore.batch()).thenReturn(batch);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of());

        int migrated = repository.migrateFeedbackReminderSent();

        assertEquals(0, migrated);
        Mockito.verifyNoInteractions(batch);
    }
}
