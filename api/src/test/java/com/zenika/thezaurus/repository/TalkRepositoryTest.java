package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TalkRepositoryTest {

    private TalkRepository repository;
    private QuerySnapshot snapshot;

    @BeforeEach
    public void setUp() {
        Firestore firestore = Mockito.mock(Firestore.class);
        CollectionReference collection = Mockito.mock(CollectionReference.class);
        snapshot = Mockito.mock(QuerySnapshot.class);

        Mockito.when(firestore.collection("talks")).thenReturn(collection);
        Mockito.when(collection.get()).thenReturn(ApiFutures.immediateFuture(snapshot));

        repository = new TalkRepository();
        repository.firestore = firestore;
        repository.collectionPrefix = Optional.empty();
    }

    private QueryDocumentSnapshot documentOf(Object rawSpeakers, DocumentReference reference) {
        QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(doc.get("speakers")).thenReturn(rawSpeakers);
        Mockito.when(doc.getReference()).thenReturn(reference);
        return doc;
    }

    @Test
    public void testMigrateLegacySpeakersRewritesOnlyLegacyDocuments() throws Exception {
        DocumentReference legacyRef = Mockito.mock(DocumentReference.class);
        Mockito.when(legacyRef.update(Mockito.eq("speakers"), Mockito.any()))
                .thenReturn(ApiFutures.immediateFuture(Mockito.mock(WriteResult.class)));
        QueryDocumentSnapshot legacyDoc = documentOf(List.of("Jane Doe"), legacyRef);

        DocumentReference migratedRef = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot migratedDoc = documentOf(List.of(Map.of("name", "John Doe")), migratedRef);

        QueryDocumentSnapshot noSpeakersDoc = documentOf(null, Mockito.mock(DocumentReference.class));

        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(legacyDoc, migratedDoc, noSpeakersDoc));

        int migrated = repository.migrateLegacySpeakers();

        assertEquals(1, migrated);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(legacyRef).update(Mockito.eq("speakers"), captor.capture());
        assertEquals(List.of(Map.of("name", "Jane Doe")), captor.getValue());
        Mockito.verifyNoInteractions(migratedRef);
    }

    @Test
    public void testMigrateLegacySpeakersConvertsOnlyStringElements() throws Exception {
        // Un document mixte (déjà partiellement migré, ou co-speaker ajouté après coup) :
        // les chaînes sont converties, les objets existants sont réémis tels quels.
        Map<String, Object> structured = Map.of("name", "John Doe", "email", "john@zenika.com");
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        Mockito.when(reference.update(Mockito.eq("speakers"), Mockito.any()))
                .thenReturn(ApiFutures.immediateFuture(Mockito.mock(WriteResult.class)));
        QueryDocumentSnapshot doc = documentOf(List.of("Jane Doe", structured), reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(1, repository.migrateLegacySpeakers());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(reference).update(Mockito.eq("speakers"), captor.capture());
        assertEquals(List.of(Map.of("name", "Jane Doe"), structured), captor.getValue());
    }

    @Test
    public void testMigrateLegacySpeakersIsIdempotent() throws Exception {
        // Après une première migration, plus aucun document ne contient de chaîne :
        // une seconde exécution ne réécrit rien et retourne 0.
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot doc =
                documentOf(List.of(Map.of("name", "Jane Doe", "email", "jane@zenika.com")), reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(0, repository.migrateLegacySpeakers());
        Mockito.verifyNoInteractions(reference);
    }

    @Test
    public void testMigrateLegacySpeakersOnEmptyCollection() throws Exception {
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of());

        assertEquals(0, repository.migrateLegacySpeakers());
    }
}
