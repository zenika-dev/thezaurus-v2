package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        User jane = User.builder().name("Jane Doe").email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT)).build();
        User john = User.builder().name("John Doe").email("john@zenika.com")
                .roles(List.of(Role.ADMIN, Role.DT)).build();
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

    // --- Migration role -> roles ---------------------------------------------------------------

    private QueryDocumentSnapshot rawDocumentOf(Object role, Object roles, DocumentReference reference) {
        QueryDocumentSnapshot doc = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(doc.get("role")).thenReturn(role);
        Mockito.when(doc.get("roles")).thenReturn(roles);
        Mockito.when(doc.getReference()).thenReturn(reference);
        return doc;
    }

    private DocumentReference updatableReference() {
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        Mockito.when(reference.update(Mockito.anyMap()))
                .thenReturn(ApiFutures.immediateFuture(Mockito.mock(WriteResult.class)));
        return reference;
    }

    @Test
    public void testMigrateLegacyRolesConvertsMembreToConsultantAndDeletesRoleField() throws Exception {
        DocumentReference reference = updatableReference();
        QueryDocumentSnapshot doc = rawDocumentOf("membre", null, reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(1, repository.migrateLegacyRoles());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(reference).update(captor.capture());
        assertEquals(FieldValue.delete(), captor.getValue().get("role"));
        assertEquals(List.of(Role.CONSULTANT.name()), captor.getValue().get("roles"));
    }

    @Test
    public void testMigrateLegacyRolesKeepsExistingRolesAndOnlyDeletesRoleField() throws Exception {
        // Un document qui a déjà des roles[] mais porte encore l'ancien champ : on ne
        // touche pas aux roles existants, on supprime seulement le champ legacy.
        DocumentReference reference = updatableReference();
        QueryDocumentSnapshot doc = rawDocumentOf("admin", List.of("DT"), reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(1, repository.migrateLegacyRoles());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(reference).update(captor.capture());
        assertEquals(FieldValue.delete(), captor.getValue().get("role"));
        assertFalse(captor.getValue().containsKey("roles"));
    }

    @Test
    public void testMigrateLegacyRolesIsIdempotent() throws Exception {
        // Après migration, plus aucun document ne porte le champ 'role' : une seconde
        // exécution ne réécrit rien et retourne 0.
        DocumentReference reference = Mockito.mock(DocumentReference.class);
        QueryDocumentSnapshot doc = rawDocumentOf(null, List.of("CONSULTANT"), reference);
        Mockito.when(snapshot.getDocuments()).thenReturn(List.of(doc));

        assertEquals(0, repository.migrateLegacyRoles());
        Mockito.verifyNoInteractions(reference);
    }
}
