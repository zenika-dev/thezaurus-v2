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

    // --- Lecture defensive : un document illisible ne fait pas echouer les autres ---------------

    @Test
    public void findAllSkipsADocumentThatFailsToDeserializeInsteadOfFailingTheWholeList() throws Exception {
        // Simule le cas reel : Conference.date est type ConferencePeriod, un document dont le
        // champ n'a pas ce format (non migre, corrompu) fait lever toObject().
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

    @Test
    public void migrateLegacyDatesRewritesASingleDate() throws Exception {
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot doc = documentOf("2026-03-12", reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(1, repository.migrateLegacyDates());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(batch).update(Mockito.eq(reference), Mockito.eq("date"), captor.capture());
        assertEquals(Map.of("start", "2026-03-12", "end", "2026-03-12", "precision", "DAY"), captor.getValue());
        Mockito.verify(batch).commit();
    }

    @Test
    public void migrateLegacyDatesRewritesARange() throws Exception {
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot doc = documentOf("2026-03-01/2026-03-03", reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(1, repository.migrateLegacyDates());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(batch).update(Mockito.eq(reference), Mockito.eq("date"), captor.capture());
        assertEquals(Map.of("start", "2026-03-01", "end", "2026-03-03", "precision", "DAY"), captor.getValue());
    }

    @Test
    public void migrateLegacyDatesIgnoresAlreadyMigratedDocuments() throws Exception {
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot doc =
                documentOf(Map.of("start", "2026-03-12", "end", "2026-03-12", "precision", "DAY"), reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(0, repository.migrateLegacyDates());
        Mockito.verifyNoInteractions(batch);
    }

    @Test
    public void migrateLegacyDatesRewritesUnrecognizedStringsAsAnEmptyPeriod() throws Exception {
        // Une chaine non reconnue est reecrite en periode vide (Map), jamais laissee en l'etat :
        // Conference.date est type ConferencePeriod, une String restante ferait echouer la
        // desertialisation Firestore de ce document a la premiere lecture.
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot doc = documentOf("not-a-date", reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(1, repository.migrateLegacyDates());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(batch).update(Mockito.eq(reference), Mockito.eq("date"), captor.capture());
        assertEquals(Map.of("start", "", "end", "", "precision", "DAY"), captor.getValue());
    }

    @Test
    public void migrateLegacyDatesOnEmptyCollection() throws Exception {
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of());

        assertEquals(0, repository.migrateLegacyDates());
        Mockito.verifyNoInteractions(batch);
    }

    @Test
    public void migrateLegacyDatesCommitsInBatchesOf500() throws Exception {
        List<QueryDocumentSnapshot> docs = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            docs.add(documentOf("2026-03-12", Mockito.mock(DocumentReference.class)));
        }
        Mockito.when(snapshot.getDocuments()).thenReturn(docs);
        WriteBatch secondBatch = Mockito.mock(WriteBatch.class);
        Mockito.when(secondBatch.update(Mockito.any(DocumentReference.class), Mockito.anyString(), Mockito.any()))
                .thenReturn(secondBatch);
        Mockito.when(secondBatch.commit())
                .thenReturn(ApiFutures.immediateFuture(List.of(Mockito.mock(WriteResult.class))));
        Mockito.when(repository.firestore.batch()).thenReturn(batch, secondBatch);

        assertEquals(501, repository.migrateLegacyDates());

        Mockito.verify(batch).commit();
        Mockito.verify(secondBatch).commit();
    }
}
