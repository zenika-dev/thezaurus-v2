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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestSecurity(user = "alice@zenika.com", roles = Role.Names.CONSULTANT)
class BlogPostAuthorsTest {
    @InjectMock
    @MockitoConfig(convertScopes = true)
    Firestore firestore;

    @Inject
    ObjectMapper mapper;

    private final Map<String, Map<String, Object>> documents = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
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
    void cannotSaveAnArticleWithoutAuthors() {
        given().contentType("application/json")
                .body("""
                    {"id":"empty","title":"Article","writers":[],"status":"DRAFT","tags":["java"]}
                    """)
                .post("/blog-posts")
                .then()
                .statusCode(400);
    }

    @Test
    void authorsSurviveCreationAndReplacementThroughTheApi() {
        given().contentType("application/json")
                .body("""
                    {"id":"new","title":"Article","writers":[{"name":"Alice","email":"alice@zenika.com"},{"name":"Bob"}],"status":"DRAFT","tags":["java"]}
                    """)
                .post("/blog-posts")
                .then()
                .statusCode(201);
        given().get("/blog-posts/new")
                .then()
                .statusCode(200)
                .body("writers[0].email", is("alice@zenika.com"))
                .body("writers[1].name", is("Bob"));
        given().contentType("application/json")
                .body("""
                    {"title":"Article modifié","writers":[{"name":"Bob","email":"bob@zenika.com"},{"name":"Externe"}],"status":"DRAFT","tags":["java"]}
                    """)
                .put("/blog-posts/new")
                .then()
                .statusCode(200);
        given().get("/blog-posts")
                .then()
                .statusCode(200)
                .body("[0].title", is("Article modifié"))
                .body("[0].writers.size()", is(2))
                .body("[0].writers[0].email", is("bob@zenika.com"))
                .body("[0].writers[1].name", is("Externe"))
                .body("[0].writers[1].email", nullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[{\"name\":\"  \"}]"})
    void invalidAuthorsCannotReplaceAnExistingArticle(String writers) {
        documents.put("existing", new HashMap<>(Map.of("title", "Titre conservé", "writers", List.of("Bob"))));
        given().contentType("application/json")
                .body("{\"title\":\"Modification\",\"writers\":" + writers
                        + ",\"status\":\"DRAFT\",\"tags\":[\"java\"]}")
                .put("/blog-posts/existing")
                .then()
                .statusCode(400);
        given().get("/blog-posts/existing").then().statusCode(200).body("title", is("Titre conservé"));
    }

    @Test
    void legacyAuthorsRemainReadableWithoutInventingEmails() {
        documents.put(
                "legacy",
                new HashMap<>(Map.of(
                        "id",
                        "legacy",
                        "title",
                        "Ancien article",
                        "writers",
                        List.of("Alice", "Bob"),
                        "status",
                        "DRAFT",
                        "tags",
                        List.of("java"))));
        given().get("/blog-posts/legacy")
                .then()
                .statusCode(200)
                .body("writers.size()", is(2))
                .body("writers[0].name", is("Alice"))
                .body("writers[1].name", is("Bob"))
                .body("writers[0].email", nullValue());
        given().get("/blog-posts")
                .then()
                .statusCode(200)
                .body("[0].writers[1].name", is("Bob"))
                .body("[0].writers[1].email", nullValue());
        given().contentType("application/json")
                .body("""
                    {"title":"Ancien article","writers":[{"name":"Alice","email":"alice@zenika.com"},{"name":"Bob"}],"status":"DRAFT","tags":["java"]}
                    """)
                .put("/blog-posts/legacy")
                .then()
                .statusCode(200);
        given().get("/blog-posts/legacy")
                .then()
                .statusCode(200)
                .body("writers[0].email", is("alice@zenika.com"))
                .body("writers[1].name", is("Bob"))
                .body("writers[1].email", nullValue());
    }
}
