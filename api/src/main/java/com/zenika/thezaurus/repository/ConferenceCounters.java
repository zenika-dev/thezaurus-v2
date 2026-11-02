package com.zenika.thezaurus.repository;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Transaction;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** All reads (including first-time initialization) must precede writes in the caller's transaction. */
final class ConferenceCounters {
    static final String VERSION_FIELD = "conferenceStatisticsVersion";
    static final long VERSION = 1L;

    record Counts(long submitted, long accepted) {
        Counts plus(Counts other) {
            return new Counts(submitted + other.submitted, accepted + other.accepted);
        }

        Counts minus(Counts other) {
            return new Counts(submitted - other.submitted, accepted - other.accepted);
        }

        boolean isZero() {
            return submitted == 0 && accepted == 0;
        }

        Map<String, Object> fields() {
            if (submitted < 0 || accepted < 0 || accepted > submitted || submitted > Integer.MAX_VALUE) {
                throw new IllegalStateException("Compteurs de conférence incohérents");
            }
            return Map.of("submittedTalksAmount", submitted, "acceptedTalksAmount", accepted, VERSION_FIELD, VERSION);
        }
    }

    static Counts contribution(Object status) {
        return switch (String.valueOf(status)) {
            case "SUBMITTED", "REJECTED" -> new Counts(1, 0);
            case "ACCEPTED", "DONE" -> new Counts(1, 1);
            default -> new Counts(0, 0);
        };
    }

    static String conferenceId(DocumentSnapshot talk) {
        Object value = talk.get("conference.id");
        return value instanceof String id && !id.isBlank() ? id : null;
    }

    static boolean initialized(DocumentSnapshot conference) {
        Long submitted = conference.getLong("submittedTalksAmount");
        Long accepted = conference.getLong("acceptedTalksAmount");
        return Long.valueOf(VERSION).equals(conference.getLong(VERSION_FIELD))
                && submitted != null
                && accepted != null
                && submitted >= 0
                && accepted >= 0
                && accepted <= submitted
                && submitted <= Integer.MAX_VALUE;
    }

    static Counts read(Transaction transaction, DocumentSnapshot conference, CollectionReference talks, String id)
            throws ExecutionException, InterruptedException {
        if (initialized(conference)) {
            return new Counts(conference.getLong("submittedTalksAmount"), conference.getLong("acceptedTalksAmount"));
        }
        // The query and version marker commit atomically with any talk mutation. On conflict,
        // Firestore retries against a fresh snapshot instead of overwriting concurrent deltas.
        var snapshot = transaction
                .get(talks.whereEqualTo("conference.id", id).select("status"))
                .get();
        Counts counts = new Counts(0, 0);
        for (DocumentSnapshot talk : snapshot.getDocuments()) counts = counts.plus(contribution(talk.get("status")));
        return counts;
    }

    static void write(Transaction transaction, DocumentReference reference, Counts counts) {
        transaction.update(reference, counts.fields());
    }

    private ConferenceCounters() {}
}
