package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class UserRepositoryTest {

    private UserRepository repository;
    private CollectionReference collection;
    private Query query;
    private QuerySnapshot snapshot;

    @BeforeEach
    public void setUp() {
        Firestore firestore = Mockito.mock(Firestore.class);
        collection = Mockito.mock(CollectionReference.class);
        query = Mockito.mock(Query.class);
        snapshot = Mockito.mock(QuerySnapshot.class);

        Mockito.when(firestore.collection("users")).thenReturn(collection);
        Mockito.when(collection.limit(Mockito.anyInt())).thenReturn(query);
        Mockito.when(collection.whereGreaterThanOrEqualTo(Mockito.anyString(), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.whereGreaterThanOrEqualTo(Mockito.anyString(), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.whereLessThanOrEqualTo(Mockito.anyString(), Mockito.any()))
                .thenReturn(query);
        Mockito.when(query.limit(Mockito.anyInt())).thenReturn(query);
        Mockito.when(query.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        Mockito.when(collection.get()).thenReturn(ApiFutures.immediateFuture(snapshot));

        repository = new UserRepository();
        repository.firestore = firestore;
    }

    private QueryDocumentSnapshot documentOf(User user) {
        QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(doc.toObject(User.class)).thenReturn(user);
        return doc;
    }

    @Test
    public void testFindAllMapsDocumentsToUsers() throws Exception {
        User jane = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        User john = User.builder()
                .name("John Doe")
                .email("john@zenika.com")
                .roles(List.of(Role.ADMIN, Role.DT))
                .build();
        // Les documents sont construits avant le when() : imbriquer un stub dans un autre
        // laisse Mockito avec un stubbing inachevé.
        QueryDocumentSnapshot janeDoc = documentOf(jane);
        QueryDocumentSnapshot johnDoc = documentOf(john);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(janeDoc, johnDoc));

        List<User> users = repository.findAll(500);

        assertEquals(2, users.size());
        assertEquals("Jane Doe", users.get(0).name());
        assertEquals("jane@zenika.com", users.get(0).email());
        assertEquals(List.of(Role.ADMIN, Role.DT), users.get(1).roles());
    }

    @Test
    public void testFindAllAppliesLimit() throws Exception {
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of());

        repository.findAll(42);

        Mockito.verify(collection).limit(42);
    }

    @Test
    public void testFindAllOnEmptyCollection() throws Exception {
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of());

        assertTrue(repository.findAll(500).isEmpty());
    }

    @Test
    public void testSearchCaseInsensitiveAndSubstringInName() throws Exception {
        User amine = User.builder()
                .name("Mohamed Amine")
                .email("mohamed.amine@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        User john = User.builder()
                .name("John Doe")
                .email("john@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();

        QueryDocumentSnapshot amineDoc = documentOf(amine);
        QueryDocumentSnapshot johnDoc = documentOf(john);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(amineDoc, johnDoc));

        List<User> results = repository.search("amine", 20);
        assertEquals(1, results.size());
        assertEquals("Mohamed Amine", results.get(0).name());

        List<User> upperResults = repository.search("AMINE", 20);
        assertEquals(1, upperResults.size());
        assertEquals("Mohamed Amine", upperResults.get(0).name());
    }

    @Test
    public void testSearchSubstringInEmail() throws Exception {
        User amine = User.builder()
                .name("Mohamed Bouzid")
                .email("mohamed.amine.bouzid@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();

        QueryDocumentSnapshot amineDoc = documentOf(amine);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(amineDoc));

        List<User> results = repository.search("amine.bouzid", 20);
        assertEquals(1, results.size());
        assertEquals("mohamed.amine.bouzid@zenika.com", results.get(0).email());
    }

    @Test
    public void testSearchAccentInsensitive() throws Exception {
        User elodie = User.builder()
                .name("Élodie François")
                .email("elodie.francois@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();

        QueryDocumentSnapshot elodieDoc = documentOf(elodie);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(elodieDoc));

        List<User> results = repository.search("elodie", 20);
        assertEquals(1, results.size());
        assertEquals("Élodie François", results.get(0).name());

        List<User> francoisResults = repository.search("francois", 20);
        assertEquals(1, francoisResults.size());
        assertEquals("Élodie François", francoisResults.get(0).name());
    }

    @Test
    public void testSearchWithoutQueryAppliesLimitOnly() throws Exception {
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of());

        repository.search(null, 20);

        Mockito.verify(collection).limit(20);
    }

    @Test
    public void testSearchSanitizesControlCharactersAndTruncatesLongInput() throws Exception {
        User alice = User.builder()
                .name("Alice Martin")
                .email("alice.martin@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        QueryDocumentSnapshot aliceDoc = documentOf(alice);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(aliceDoc));

        String malicious = "Alice\u0000\r\n\t";
        List<User> results = repository.search(malicious, 20);

        assertEquals(1, results.size());
        assertEquals("Alice Martin", results.get(0).name());
    }

    // --- Écritures ciblées ---------------------------------------------------------------------
    // update(Map) et jamais set(), qui écrase le document entier et perdrait roles/slackUserId.

    private DocumentReference documentReferenceFor(String email) {
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        Mockito.when(reference.update(Mockito.anyMap()))
                .thenReturn(ApiFutures.immediateFuture(Mockito.mock(WriteResult.class)));
        Mockito.when(collection.document(email)).thenReturn(reference);
        return reference;
    }

    private Map<String, Object> capturedUpdateData(DocumentReference reference) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(reference).update(captor.capture());
        return captor.getValue();
    }

    @Test
    public void testUpdateNotificationPreferencesWritesOnlyPreferenceFields() throws Exception {
        DocumentReference reference = documentReferenceFor("jane@zenika.com");

        repository.updateNotificationPreferences("jane@zenika.com", true, false);

        Map<String, Object> written = capturedUpdateData(reference);
        assertEquals(Set.of("emailNotificationsEnabled", "slackNotificationsEnabled"), written.keySet());
        assertEquals(true, written.get("emailNotificationsEnabled"));
        assertEquals(false, written.get("slackNotificationsEnabled"));
        // roles et slackUserId hors de la charge écrite, et aucun set().
        Mockito.verify(reference, Mockito.never()).set(Mockito.any());
    }

    @Test
    public void testUpdateSlackUserIdWritesOnlySlackUserIdField() throws Exception {
        DocumentReference reference = documentReferenceFor("jane@zenika.com");

        repository.updateSlackUserId("jane@zenika.com", "U123");

        Map<String, Object> written = capturedUpdateData(reference);
        assertEquals(Set.of("slackUserId"), written.keySet());
        assertEquals("U123", written.get("slackUserId"));
        Mockito.verify(reference, Mockito.never()).set(Mockito.any());
    }

    @Test
    public void testUpdateNameWritesOnlyNameField() throws Exception {
        DocumentReference reference = documentReferenceFor("jane@zenika.com");

        repository.updateName("jane@zenika.com", "Jane Doe");

        Map<String, Object> written = capturedUpdateData(reference);
        assertEquals(Set.of("name"), written.keySet());
        assertEquals("Jane Doe", written.get("name"));
        Mockito.verify(reference, Mockito.never()).set(Mockito.any());
    }
}
