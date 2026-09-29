package com.zenika.thezaurus.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import jakarta.ws.rs.WebApplicationException;
import java.util.concurrent.ExecutionException;

/** Préserve les erreurs métier HTTP à travers l'enveloppe asynchrone de Firestore. */
final class FirestoreTransactions {
    private FirestoreTransactions() {}

    static <T> T run(Firestore firestore, Transaction.Function<T> operation)
            throws ExecutionException, InterruptedException {
        try {
            return firestore.runTransaction(operation).get();
        } catch (ExecutionException exception) {
            for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof WebApplicationException http) throw http;
            }
            throw exception;
        }
    }
}
