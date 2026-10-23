package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
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
        Mockito.when(collection.whereEqualTo(Mockito.anyString(), Mockito.any()))
                .thenReturn(query);
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
        Mockito.when(reference.update(Mockito.anyString(), Mockito.any()))
                .thenReturn(ApiFutures.immediateFuture(Mockito.mock(WriteResult.class)));
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

    @Test
    public void photoConsentAndRoleUpdatesPreserveUnrelatedFields() throws Exception {
        DocumentReference reference = documentReferenceFor("jane@zenika.com");
        repository.updateAiPhotoAllowed("jane@zenika.com", true);
        repository.updateCustomPhoto("jane@zenika.com", "custom");
        repository.updateRoles("jane@zenika.com", List.of(Role.DT));
        Mockito.verify(reference).update("aiPhotoAllowed", true);
        Mockito.verify(reference).update("customPhotoObject", "custom");
        Mockito.verify(reference).update("roles", List.of(Role.DT));
        Mockito.verify(reference, Mockito.never()).set(Mockito.any());
    }

    @Test
    public void googleSynchronizationChecksTheCurrentPhotoInsideTheTransaction() throws Exception {
        DocumentReference reference = documentReferenceFor("jane@zenika.com");
        Transaction transaction = Mockito.mock(Transaction.class);
        DocumentSnapshot snapshot = Mockito.mock(DocumentSnapshot.class);
        Mockito.when(transaction.get(reference)).thenReturn(ApiFutures.immediateFuture(snapshot));
        Mockito.when(repository.firestore.runTransaction(Mockito.<Transaction.Function<Void>>any()))
                .thenAnswer(invocation -> {
                    Transaction.Function<Void> function = invocation.getArgument(0);
                    return ApiFutures.immediateFuture(function.updateCallback(transaction));
                });
        Mockito.when(snapshot.toObject(User.class))
                .thenReturn(User.builder().customPhotoObject("custom").build());
        repository.syncGooglePhoto("jane@zenika.com", "https://lh3.googleusercontent.com/new");
        Mockito.verify(transaction, Mockito.never()).update(Mockito.eq(reference), Mockito.anyString(), Mockito.any());
        Mockito.when(snapshot.toObject(User.class)).thenReturn(User.builder().build());
        repository.syncGooglePhoto("jane@zenika.com", "https://lh3.googleusercontent.com/new");
        Mockito.verify(transaction).update(reference, "googlePhotoUrl", "https://lh3.googleusercontent.com/new");
    }

    @Test
    public void testFindBySlackUserId() throws Exception {
        User jane = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .slackUserId("U123")
                .build();
        QueryDocumentSnapshot doc = documentOf(jane);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        User result = repository.findBySlackUserId("U123");
        assertEquals("jane@zenika.com", result.email());
        assertEquals("Jane Doe", result.name());
        assertEquals("U123", result.slackUserId());
    }

    @Test
    public void testFindBySlackUserIdNullOrBlank() throws Exception {
        assertNull(repository.findBySlackUserId(null));
        assertNull(repository.findBySlackUserId("   "));
    }

    @Test
    public void testFindByEmailsWithEmptyListReturnsEmptyMap() throws Exception {
        Map<String, User> result = repository.findByEmails(List.of());
        assertTrue(result.isEmpty());
        Map<String, User> nullResult = repository.findByEmails(null);
        assertTrue(nullResult.isEmpty());
    }

    @Test
    public void testFindByEmailsBatchesViaGetAll() throws Exception {
        DocumentReference janeRef = documentReferenceFor("jane@zenika.com");
        DocumentReference johnRef = documentReferenceFor("john@zenika.com");

        com.google.cloud.firestore.DocumentSnapshot janeDoc =
                Mockito.mock(com.google.cloud.firestore.DocumentSnapshot.class);
        Mockito.when(janeDoc.exists()).thenReturn(true);
        Mockito.when(janeDoc.getId()).thenReturn("jane@zenika.com");
        User jane = User.builder().name("Jane Updated").email("jane@zenika.com").build();
        Mockito.when(janeDoc.toObject(User.class)).thenReturn(jane);

        com.google.cloud.firestore.DocumentSnapshot johnDoc =
                Mockito.mock(com.google.cloud.firestore.DocumentSnapshot.class);
        Mockito.when(johnDoc.exists()).thenReturn(false);

        Mockito.when(repository.firestore.getAll(Mockito.any(DocumentReference[].class)))
                .thenReturn(ApiFutures.immediateFuture(List.of(janeDoc, johnDoc)));

        Map<String, User> result = repository.findByEmails(List.of("jane@zenika.com", "john@zenika.com"));
        assertEquals(1, result.size());
        assertEquals("Jane Updated", result.get("jane@zenika.com").name());
    }
}
