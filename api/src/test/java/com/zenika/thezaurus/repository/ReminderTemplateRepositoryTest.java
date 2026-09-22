package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.zenika.thezaurus.exception.ThezaurusException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReminderTemplateRepositoryTest {
    ReminderTemplateRepository repository = new ReminderTemplateRepository();
    Firestore firestore = mock(Firestore.class);
    CollectionReference collection = mock(CollectionReference.class);
    DocumentReference reference = mock(DocumentReference.class);
    DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
    Transaction transaction = mock(Transaction.class);

    @BeforeEach
    void setUp() throws Exception {
        repository.firestore = firestore;
        repository.collectionPrefix = Optional.of("test");
        when(firestore.collection("test_app_configuration")).thenReturn(collection);
        when(collection.document("reminder_template")).thenReturn(reference);
        when(reference.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(transaction.get(reference)).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<?> function = invocation.getArgument(0);
            try {
                return ApiFutures.immediateFuture(function.updateCallback(transaction));
            } catch (Exception exception) {
                return ApiFutures.immediateFailedFuture(exception);
            }
        });
    }

    @Test
    void missingDocumentReturnsEmptyRevisionZero() throws Exception {
        var result = repository.get();
        assertEquals("", result.subject());
        assertEquals("", result.bodyHtml());
        assertEquals(0, result.revision());
    }

    @Test
    void firstSaveChecksAndWritesInSameTransaction() throws Exception {
        var result = repository.save("Subject", "<p>Body</p>", 0);
        assertEquals(1, result.revision());
        var ordered = inOrder(transaction);
        ordered.verify(transaction).get(reference);
        ordered.verify(transaction)
                .set(reference, Map.of("subject", "Subject", "bodyHtml", "<p>Body</p>", "revision", 1L));
        verify(reference, never()).set(any());
    }

    @Test
    void existingDocumentWithMissingFieldsCanBeReadAndSaved() throws Exception {
        when(snapshot.exists()).thenReturn(true);
        // Mockito's default boxed Long is zero: return null explicitly as Firestore does.
        when(snapshot.getLong("revision")).thenReturn(null);
        var current = repository.get();
        assertEquals("", current.subject());
        assertEquals("", current.bodyHtml());
        assertEquals(0, current.revision());
        assertEquals(
                1, repository.save("Sujet", "<p>Corps</p>", current.revision()).revision());
    }

    @Test
    void staleRevisionNeverWrites() throws Exception {
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getLong("revision")).thenReturn(2L);
        var error = assertThrows(ThezaurusException.class, () -> repository.save("Subject", "Body", 1));
        assertEquals(409, error.getStatus().getStatusCode());
        verify(transaction, never()).set(any(), any());
    }

    @Test
    void transactionRetryRechecksConcurrentRevision() throws Exception {
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getLong("revision")).thenReturn(1L, 2L);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<?> function = invocation.getArgument(0);
            function.updateCallback(transaction); // first attempt aborted by Firestore after competing commit
            try {
                return ApiFutures.immediateFuture(function.updateCallback(transaction));
            } catch (Exception exception) {
                return ApiFutures.immediateFailedFuture(new ExecutionException(exception));
            }
        });
        assertThrows(ThezaurusException.class, () -> repository.save("Subject", "Body", 1));
        verify(transaction, times(2)).get(reference);
        verify(transaction, times(1)).set(any(), any());
    }
}
