package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConferenceStatisticsTest {
    @Test
    void groupsProjectedTalksWithoutReadingAnyConference() throws Exception {
        TalkRepository repository = new TalkRepository();
        repository.firestore = mock(Firestore.class);
        repository.collectionPrefix = Optional.of("test");
        CollectionReference collection = mock(CollectionReference.class);
        Query filtered = mock(Query.class);
        Query projected = mock(Query.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(repository.firestore.collection("test_talks")).thenReturn(collection);
        when(collection.whereIn("status", List.of("SUBMITTED", "ACCEPTED", "REJECTED", "DONE")))
                .thenReturn(filtered);
        when(filtered.select("conference.id", "status")).thenReturn(projected);
        when(projected.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        List<QueryDocumentSnapshot> documents = List.of(
                talk("a", "SUBMITTED"),
                talk("a", "ACCEPTED"),
                talk("a", "DONE"),
                talk("a", "REJECTED"),
                talk("b", "SUBMITTED"),
                talk(null, "ACCEPTED"),
                talk(" ", "DONE"));
        when(snapshot.getDocuments()).thenReturn(documents);

        assertEquals(
                java.util.Map.of(
                        "a", new TalkRepository.ConferenceStatistics(4, 2),
                        "b", new TalkRepository.ConferenceStatistics(1, 0)),
                repository.statisticsByConference());
        verify(projected, times(1)).get();
        verify(repository.firestore, times(1)).collection("test_talks");
        verifyNoMoreInteractions(repository.firestore);
        verify(collection, never()).get();
    }

    private QueryDocumentSnapshot talk(String id, String status) {
        QueryDocumentSnapshot document = mock(QueryDocumentSnapshot.class);
        when(document.get("conference.id")).thenReturn(id);
        when(document.get("status")).thenReturn(status);
        return document;
    }
}
