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

    // --- Lecture defensive : un document illisible ne fait pas echouer les autres ---------------
    //
    // Cas reel : une Conference embarquee dans un talk (cree via la commande Slack avant
    // l'introduction de ConferencePeriod) peut porter un `date` au format legacy (String), que
    // migrateLegacyDates ne touche pas (elle ne migre que la collection conferences).

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
