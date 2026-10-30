package com.zenika.thezaurus.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.zenika.thezaurus.model.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Exercises repository transactions against staged writes: failed attempts never commit. */
class TransactionalConferenceCountersTest {
    final Map<String, Map<String, Object>> data = new HashMap<>();
    final Map<String, DocumentReference> references = new HashMap<>();
    final List<Runnable> pending = new ArrayList<>();
    final List<String> conferenceWrites = new ArrayList<>();
    Firestore firestore;
    CollectionReference talks;
    CollectionReference conferences;
    TalkRepository repository;
    ConferenceRepository conferenceRepository;
    boolean writing;
    boolean retry;
    int scans;
    String failScanFor;
    Runnable beforeRetry = () -> {};

    @BeforeEach
    void setup() throws Exception {
        firestore = mock(Firestore.class);
        talks = collection("test_talks");
        conferences = collection("test_conferences");
        when(firestore.collection("test_talks")).thenReturn(talks);
        when(firestore.collection("test_conferences")).thenReturn(conferences);
        repository = new TalkRepository();
        repository.firestore = firestore;
        repository.collectionPrefix = Optional.of("test");
        conferenceRepository = new ConferenceRepository();
        conferenceRepository.firestore = firestore;
        conferenceRepository.collectionPrefix = Optional.of("test");
        Transaction transaction = mock(Transaction.class);
        when(transaction.get(any(DocumentReference.class))).thenAnswer(invocation -> {
            assertFalse(writing, "Firestore forbids reading after the first write");
            DocumentReference reference = invocation.getArgument(0);
            return ApiFutures.immediateFuture(snapshot(reference.getPath()));
        });
        when(transaction.get(any(Query.class))).thenAnswer(invocation -> {
            assertFalse(writing, "Query after write");
            Query query = invocation.getArgument(0);
            return ApiFutures.immediateFuture(query.get().get());
        });
        when(transaction.update(any(DocumentReference.class), anyMap())).thenAnswer(invocation -> {
            writing = true;
            DocumentReference reference = invocation.getArgument(0);
            Map<String, Object> fields = new HashMap<>(invocation.getArgument(1));
            pending.add(() -> {
                if (!data.containsKey(reference.getPath())) throw new IllegalStateException("Missing document");
                data.get(reference.getPath()).putAll(fields);
                if (reference.getPath().startsWith("test_conferences/")) conferenceWrites.add(reference.getPath());
            });
            return transaction;
        });
        when(transaction.set(any(DocumentReference.class), anyMap())).thenAnswer(invocation -> {
            writing = true;
            DocumentReference reference = invocation.getArgument(0);
            Map<String, Object> fields = new HashMap<>(invocation.getArgument(1));
            pending.add(() -> data.put(reference.getPath(), fields));
            return transaction;
        });
        when(transaction.create(any(DocumentReference.class), anyMap())).thenAnswer(invocation -> {
            writing = true;
            DocumentReference reference = invocation.getArgument(0);
            Map<String, Object> fields = new HashMap<>(invocation.getArgument(1));
            if (data.containsKey(reference.getPath())) throw new IllegalStateException("Already exists");
            pending.add(() -> data.put(reference.getPath(), fields));
            return transaction;
        });
        when(transaction.delete(any(DocumentReference.class))).thenAnswer(invocation -> {
            writing = true;
            DocumentReference reference = invocation.getArgument(0);
            pending.add(() -> data.remove(reference.getPath()));
            return transaction;
        });
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<?> callback = invocation.getArgument(0);
            pending.clear();
            writing = false;
            Object result;
            try {
                result = callback.updateCallback(transaction);
                if (retry) {
                    retry = false;
                    pending.clear();
                    writing = false;
                    beforeRetry.run();
                    result = callback.updateCallback(transaction);
                }
                pending.forEach(Runnable::run);
            } finally {
                pending.clear();
                writing = false;
            }
            return ApiFutures.immediateFuture(result);
        });
    }

    CollectionReference collection(String name) {
        CollectionReference collection = mock(CollectionReference.class);
        when(collection.document(anyString())).thenAnswer(i -> reference(name + "/" + i.getArgument(0)));
        if (name.endsWith("talks")) {
            when(collection.whereEqualTo(eq("conference.id"), anyString())).thenAnswer(i -> {
                String id = i.getArgument(1);
                Query query = mock(Query.class, RETURNS_SELF);
                when(query.get()).thenAnswer(ignored -> {
                    scans++;
                    if (id.equals(failScanFor)) throw new IllegalStateException("Simulated interruption");
                    List<QueryDocumentSnapshot> documents = data.entrySet().stream()
                            .filter(e -> e.getKey().startsWith("test_talks/"))
                            .filter(e -> e.getValue().get("conference") instanceof Map<?, ?> conf
                                    && id.equals(conf.get("id")))
                            .map(e -> snapshot(e.getKey()))
                            .toList();
                    QuerySnapshot result = mock(QuerySnapshot.class);
                    when(result.getDocuments()).thenReturn(documents);
                    return ApiFutures.immediateFuture(result);
                });
                return query;
            });
        } else {
            Query query = mock(Query.class);
            when(collection.select(ConferenceCounters.VERSION_FIELD, "deleting"))
                    .thenReturn(query);
            when(query.get()).thenAnswer(i -> {
                List<QueryDocumentSnapshot> docs = data.keySet().stream()
                        .filter(key -> key.startsWith(name + "/"))
                        .sorted()
                        .map(this::snapshot)
                        .toList();
                QuerySnapshot result = mock(QuerySnapshot.class);
                when(result.getDocuments()).thenReturn(docs);
                return ApiFutures.immediateFuture(result);
            });
        }
        return collection;
    }

    DocumentReference reference(String path) {
        return references.computeIfAbsent(path, key -> {
            DocumentReference reference = mock(DocumentReference.class);
            when(reference.getPath()).thenReturn(key);
            when(reference.create(anyMap())).thenAnswer(i -> {
                if (data.containsKey(key)) throw new IllegalStateException("Already exists");
                data.put(key, new HashMap<>(i.getArgument(0)));
                return ApiFutures.immediateFuture(null);
            });
            return reference;
        });
    }

    QueryDocumentSnapshot snapshot(String path) {
        Map<String, Object> fields = new HashMap<>(data.getOrDefault(path, Map.of()));
        QueryDocumentSnapshot document = mock(QueryDocumentSnapshot.class);
        when(document.exists()).thenReturn(data.containsKey(path));
        when(document.getId()).thenReturn(path.substring(path.indexOf('/') + 1));
        DocumentReference reference = reference(path);
        when(document.getReference()).thenReturn(reference);
        when(document.get(anyString())).thenAnswer(i -> {
            String key = i.getArgument(0);
            return key.equals("conference.id")
                    ? ((Map<?, ?>) fields.getOrDefault("conference", Map.of())).get("id")
                    : fields.get(key);
        });
        when(document.getLong(anyString())).thenAnswer(i -> {
            Object value = fields.get(i.getArgument(0));
            return value instanceof Number n ? n.longValue() : null;
        });
        when(document.getBoolean(anyString())).thenAnswer(i -> fields.get(i.getArgument(0)));
        String id = path.substring(path.indexOf('/') + 1);
        when(document.toObject(Conference.class)).thenReturn(new Conference(id, "Conference", null));
        when(document.toObject(Talk.class)).thenReturn(new Talk(id, "Talk", "Description"));
        return document;
    }

    void conference(String id, long submitted, long accepted) {
        data.put(
                "test_conferences/" + id,
                new HashMap<>(Map.of(
                        "name",
                        "Conference",
                        "submittedTalksAmount",
                        submitted,
                        "acceptedTalksAmount",
                        accepted,
                        ConferenceCounters.VERSION_FIELD,
                        1L)));
    }

    void storedTalk(String id, String conference, String status) {
        data.put("test_talks/" + id, new HashMap<>(Map.of("conference", Map.of("id", conference), "status", status)));
    }

    Talk talk(String conference, TalkStatus status) {
        return new Talk("Talk", "Description", List.of(), "Nantes", status, Visibility.PUBLIC)
                .withId("talk")
                .withConference(conference == null ? null : new Conference(conference, "Conference", null));
    }

    void counts(String id, long submitted, long accepted) {
        Map<String, Object> fields = data.get("test_conferences/" + id);
        assertEquals(submitted, ((Number) fields.get("submittedTalksAmount")).longValue());
        assertEquals(accepted, ((Number) fields.get("acceptedTalksAmount")).longValue());
        assertEquals(1L, fields.get(ConferenceCounters.VERSION_FIELD));
    }

    @ParameterizedTest
    @CsvSource({"DRAFT,0,0", "PLANNED,0,0", "SUBMITTED,1,0", "REJECTED,1,0", "ACCEPTED,1,1", "DONE,1,1"})
    void creationCountsEachStatus(String status, long submitted, long accepted) throws Exception {
        conference("a", 0, 0);
        repository.create(talk("a", TalkStatus.valueOf(status)));
        counts("a", submitted, accepted);
        assertEquals(0, scans);
    }

    @ParameterizedTest
    @CsvSource({
        "SUBMITTED,ACCEPTED,1,1",
        "ACCEPTED,DONE,1,1",
        "ACCEPTED,REJECTED,1,0",
        "DONE,DRAFT,0,0",
        "DRAFT,SUBMITTED,1,0",
        "SUBMITTED,PLANNED,0,0"
    })
    void statusChangesApplyOnlyTheDifference(String before, String after, long submitted, long accepted)
            throws Exception {
        var initial = ConferenceCounters.contribution(before);
        conference("a", initial.submitted(), initial.accepted());
        storedTalk("talk", "a", before);
        repository.update("talk", talk("a", TalkStatus.valueOf(after)), ignored -> true);
        counts("a", submitted, accepted);
        if (before.equals("ACCEPTED") && after.equals("DONE")) assertTrue(conferenceWrites.isEmpty());
        assertEquals(0, scans);
    }

    @Test
    void movingAnAcceptedTalkUpdatesBothConferencesAndDetachingRemovesIt() throws Exception {
        conference("a", 1, 1);
        conference("b", 0, 0);
        storedTalk("talk", "a", "ACCEPTED");
        repository.update("talk", talk("b", TalkStatus.DONE), ignored -> true);
        counts("a", 0, 0);
        counts("b", 1, 1);
        repository.update("talk", talk(null, TalkStatus.DONE), ignored -> true);
        counts("b", 0, 0);
    }

    @Test
    void deletingTwiceDecrementsOnlyOnce() throws Exception {
        conference("a", 1, 1);
        storedTalk("talk", "a", "DONE");
        repository.delete("talk");
        repository.delete("talk");
        counts("a", 0, 0);
        assertEquals(1, conferenceWrites.size());
    }

    @Test
    void deletingAnOrphanDoesNotRecreateItsConference() throws Exception {
        storedTalk("talk", "missing", "ACCEPTED");
        repository.delete("talk");
        assertTrue(data.isEmpty());
    }

    @Test
    void retriesDoNotDoubleCountAndReadFreshState() throws Exception {
        conference("a", 1, 0);
        storedTalk("talk", "a", "SUBMITTED");
        retry = true;
        beforeRetry = () -> {
            storedTalk("other", "a", "ACCEPTED");
            conference("a", 2, 1);
        };
        repository.update("talk", talk("a", TalkStatus.ACCEPTED), ignored -> true);
        counts("a", 2, 2);
        assertEquals(1, conferenceWrites.size());
    }

    @Test
    void unauthorizedAndDuplicateCreatesLeaveCountersUntouched() throws Exception {
        conference("a", 1, 0);
        storedTalk("talk", "a", "SUBMITTED");
        assertThrows(
                jakarta.ws.rs.WebApplicationException.class,
                () -> repository.update("talk", talk("a", TalkStatus.ACCEPTED), ignored -> false));
        assertThrows(IllegalStateException.class, () -> repository.create(talk("a", TalkStatus.ACCEPTED)));
        counts("a", 1, 0);
        assertTrue(conferenceWrites.isEmpty());
    }

    @Test
    void migrationReplacesLegacyValuesAndIsIdempotent() throws Exception {
        data.put("test_conferences/a", new HashMap<>(Map.of("name", "A", "submittedTalksAmount", 99)));
        data.put("test_conferences/b", new HashMap<>(Map.of("name", "B")));
        storedTalk("one", "a", "ACCEPTED");
        storedTalk("two", "a", "REJECTED");
        storedTalk("draft", "a", "DRAFT");
        assertEquals(2, conferenceRepository.migrateStatistics());
        counts("a", 2, 1);
        counts("b", 0, 0);
        assertEquals(0, conferenceRepository.migrateStatistics());
        assertEquals(2, scans);
    }

    @Test
    void mutationInitializesLegacyConferenceInTheSameTransaction() throws Exception {
        data.put("test_conferences/a", new HashMap<>(Map.of("name", "A", "submittedTalksAmount", 99)));
        storedTalk("talk", "a", "SUBMITTED");
        storedTalk("other", "a", "DONE");
        repository.update("talk", talk("a", TalkStatus.ACCEPTED), ignored -> true);
        counts("a", 2, 2);
        assertEquals(0, conferenceRepository.migrateStatistics());
    }

    @Test
    void editingConferenceCannotOverrideCountersOrDropTheVersion() throws Exception {
        conference("a", 4, 2);
        Conference input = new Conference("a", "Renamed", null);
        input.setSubmittedTalksAmount(999);
        input.setAcceptedTalksAmount(999);
        Conference result = conferenceRepository.update("a", input);
        counts("a", 4, 2);
        assertEquals(4, result.getSubmittedTalksAmount());
        assertEquals(0, scans);
    }

    @Test
    void creatingConferenceIgnoresClientCounters() throws Exception {
        Conference input = new Conference("a", "A", null);
        input.setSubmittedTalksAmount(999);
        input.setAcceptedTalksAmount(999);
        conferenceRepository.create(input);
        counts("a", 0, 0);
    }

    @Test
    void deletionInitializesAnUnmigratedConferenceBeforeSubtracting() throws Exception {
        data.put("test_conferences/a", new HashMap<>(Map.of("name", "A", "submittedTalksAmount", 99)));
        storedTalk("talk", "a", "ACCEPTED");
        storedTalk("other", "a", "REJECTED");
        repository.delete("talk");
        counts("a", 1, 0);
        assertEquals(1, scans);
    }

    @Test
    void attachingAFreeTalkIncrementsTheTarget() throws Exception {
        conference("b", 0, 0);
        data.put("test_talks/talk", new HashMap<>(Map.of("conference", Map.of("name", "Free"), "status", "ACCEPTED")));
        repository.update("talk", talk("b", TalkStatus.ACCEPTED), ignored -> true);
        counts("b", 1, 1);
    }

    @Test
    void deletingADraftAndEditingATitleDoNotWriteCounters() throws Exception {
        conference("a", 0, 0);
        storedTalk("talk", "a", "DRAFT");
        repository.update("talk", talk("a", TalkStatus.DRAFT), ignored -> true);
        repository.delete("talk");
        counts("a", 0, 0);
        assertTrue(conferenceWrites.isEmpty());
    }

    @Test
    void unavailableTargetRejectsTheWholeMove() throws Exception {
        conference("a", 1, 1);
        conference("b", 0, 0);
        storedTalk("talk", "a", "ACCEPTED");
        data.get("test_conferences/b").put("deleting", true);
        assertThrows(
                jakarta.ws.rs.WebApplicationException.class,
                () -> repository.update("talk", talk("b", TalkStatus.ACCEPTED), ignored -> true));
        counts("a", 1, 1);
        counts("b", 0, 0);
        assertTrue(conferenceWrites.isEmpty());
        assertThrows(
                jakarta.ws.rs.WebApplicationException.class,
                () -> repository.update("talk", talk("missing", TalkStatus.ACCEPTED), ignored -> true));
    }

    @Test
    void interruptedMigrationResumesWithoutRecountingCompletedConferences() throws Exception {
        data.put("test_conferences/a", new HashMap<>(Map.of("name", "A")));
        data.put("test_conferences/b", new HashMap<>(Map.of("name", "B")));
        storedTalk("one", "a", "ACCEPTED");
        storedTalk("two", "b", "SUBMITTED");
        failScanFor = "b";
        assertThrows(IllegalStateException.class, () -> conferenceRepository.migrateStatistics());
        counts("a", 1, 1);
        assertFalse(data.get("test_conferences/b").containsKey(ConferenceCounters.VERSION_FIELD));
        failScanFor = null;
        assertEquals(1, conferenceRepository.migrateStatistics());
        counts("b", 1, 0);
        assertEquals(1, Collections.frequency(conferenceWrites, "test_conferences/a"));
    }

    @Test
    void migrationRetryRecountsConcurrentTalkChanges() throws Exception {
        data.put("test_conferences/a", new HashMap<>(Map.of("name", "A")));
        storedTalk("one", "a", "ACCEPTED");
        retry = true;
        beforeRetry = () -> storedTalk("two", "a", "SUBMITTED");
        assertEquals(1, conferenceRepository.migrateStatistics());
        counts("a", 2, 1);
        assertEquals(1, conferenceWrites.size());
        assertEquals(2, scans);
    }

    @Test
    void migrationSkipsConferencesBeingDeleted() throws Exception {
        data.put("test_conferences/a", new HashMap<>(Map.of("name", "A", "deleting", true)));
        assertEquals(0, conferenceRepository.migrateStatistics());
        assertEquals(0, scans);
    }
}
