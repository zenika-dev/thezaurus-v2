package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.Role;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestSecurity(user = "alice@zenika.com", roles = Role.Names.CONSULTANT)
class BlogPostWorkflowTest {
    @InjectMock
    @MockitoConfig(convertScopes = true)
    Clock clock;

    @Test
    void todayUsesParisEvenWhenUtcIsStillThePreviousDay() {
        var post = new HashMap<String, Object>(Map.of(
                "id",
                "midnight",
                "title",
                "Article",
                "writers",
                List.of(Map.of("name", "Alice")),
                "status",
                "PUBLISHED",
                "link",
                "https://blog.zenika.com/article",
                "creationDate",
                "2030-01-21",
                "actualPublicationDate",
                "2030-01-21"));
        given().contentType("application/json")
                .body(post)
                .post("/blog-posts")
                .then()
                .statusCode(201);
        post.put("actualPublicationDate", "2030-01-22");
        given().contentType("application/json")
                .body(post)
                .put("/blog-posts/midnight")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @CsvSource({
        "creationDate,2999-01-01",
        "actualPublicationDate,2999-01-01",
        "creationDate,2026-02-30",
        "publicationDate,not-a-date",
        "actualPublicationDate,2026-02-30T00:00:00",
        "link,javascript:alert(1)",
        "googleDocDraftLink,not-a-url",
        "office,unknown"
    })
    void invalidValuesAreRejectedOnCreateAndUpdate(String field, String value) {
        var post = new HashMap<String, Object>(Map.of(
                "id",
                "valid",
                "title",
                "Article",
                "writers",
                List.of(Map.of("name", "Alice")),
                "status",
                "DRAFT",
                "tags",
                List.of()));
        given().contentType("application/json")
                .body(post)
                .post("/blog-posts")
                .then()
                .statusCode(201);
        post.put(field, value);
        given().contentType("application/json")
                .body(post)
                .post("/blog-posts")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(post)
                .put("/blog-posts/valid")
                .then()
                .statusCode(400);
        given().get("/blog-posts/valid").then().statusCode(200).body(field, nullValue());
    }

    @Test
    void publicationNeedsAPublicUrlAndAnActualDateAndPreservesBothDatesAndOffice() {
        var post = new HashMap<String, Object>(Map.of(
                "id",
                "published",
                "title",
                "Article",
                "writers",
                List.of(Map.of("name", "Alice")),
                "status",
                "PUBLISHED",
                "tags",
                List.of(),
                "office",
                "nantes",
                "creationDate",
                "2026-01-20T00:00:00",
                "publicationDate",
                "2026-01-12T00:00:00"));
        given().contentType("application/json")
                .body(post)
                .post("/blog-posts")
                .then()
                .statusCode(400);
        post.put("link", "https://blog.zenika.com/article");
        given().contentType("application/json")
                .body(post)
                .post("/blog-posts")
                .then()
                .statusCode(400);
        post.put("actualPublicationDate", "2026-01-10T00:00:00");
        given().contentType("application/json")
                .body(post)
                .post("/blog-posts")
                .then()
                .statusCode(201);
        given().get("/blog-posts/published")
                .then()
                .statusCode(200)
                .body("office", is("nantes"))
                .body("publicationDate", is("2026-01-12T00:00:00"))
                .body("actualPublicationDate", is("2026-01-10T00:00:00"));
        post.put("status", "IDEA");
        given().contentType("application/json")
                .body(post)
                .put("/blog-posts/published")
                .then()
                .statusCode(200);
        given().get("/blog-posts/published")
                .then()
                .statusCode(200)
                .body("status", is("IDEA"))
                .body("office", is("nantes"))
                .body("link", is("https://blog.zenika.com/article"))
                .body("publicationDate", is("2026-01-12T00:00:00"))
                .body("actualPublicationDate", is("2026-01-10T00:00:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVIEW", "READY_TO_PUBLISH"})
    void reviewStagesRequireALinkToTheText(String status) {
        String payload = """
            {"id":"review","title":"Article","writers":[{"name":"Alice"}],"status":"%s","tags":[]%s}
            """;
        given().contentType("application/json")
                .body(payload.formatted(status, ""))
                .post("/blog-posts")
                .then()
                .statusCode(400);
        given().contentType("application/json")
                .body(payload.formatted(
                        status, ",\"googleDocDraftLink\":\"https://docs.google.com/document/d/article\""))
                .post("/blog-posts")
                .then()
                .statusCode(201);
    }

    @InjectMock
    @MockitoConfig(convertScopes = true)
    Firestore firestore;

    @Inject
    ObjectMapper mapper;

    private final Map<String, Map<String, Object>> documents = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Paris"));
        when(clock.instant()).thenReturn(Instant.parse("2030-01-20T23:30:00Z"));
        documents.clear();
        var collection = mock(CollectionReference.class);
        when(firestore.collection(anyString())).thenReturn(collection);
        when(collection.get()).thenAnswer(read -> {
            var snapshot = mock(QuerySnapshot.class);
            var entries = documents.entrySet().stream()
                    .map(entry -> {
                        var document = mock(QueryDocumentSnapshot.class);
                        when(document.getId()).thenReturn(entry.getKey());
                        when(document.getData()).thenReturn(entry.getValue());
                        return document;
                    })
                    .toList();
            when(snapshot.getDocuments()).thenReturn(entries);
            return ApiFutures.immediateFuture(snapshot);
        });
        when(collection.document(anyString())).thenAnswer(call -> {
            String id = call.getArgument(0);
            var reference = mock(DocumentReference.class);
            when(reference.get()).thenAnswer(read -> {
                var snapshot = mock(DocumentSnapshot.class);
                when(snapshot.exists()).thenReturn(documents.containsKey(id));
                when(snapshot.getId()).thenReturn(id);
                when(snapshot.getData()).thenReturn(documents.get(id));
                when(snapshot.toObject(BlogPost.class))
                        .thenAnswer(convert -> mapper.convertValue(documents.get(id), BlogPost.class));
                return ApiFutures.immediateFuture(snapshot);
            });
            when(reference.set(any(BlogPost.class))).thenAnswer(write -> {
                documents.put(
                        id, mapper.convertValue(write.getArgument(0), new TypeReference<Map<String, Object>>() {}));
                return ApiFutures.immediateFuture(null);
            });
            return reference;
        });
    }

    @Test
    void anIdeaOnlyNeedsATitleAndAnAuthor() {
        given().contentType("application/json")
                .body("""
                    {"id":"idea","title":"Une idée","writers":[{"name":"Alice"}],"status":"IDEA","tags":[],"creationDate":null}
                    """)
                .post("/blog-posts")
                .then()
                .statusCode(201);
        given().get("/blog-posts/idea")
                .then()
                .statusCode(200)
                .body("status", is("IDEA"))
                .body("creationDate", nullValue())
                .body("tags.size()", is(0));
    }
}
