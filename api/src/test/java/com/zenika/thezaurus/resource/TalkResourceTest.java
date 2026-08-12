package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class TalkResourceTest {

    @InjectMock
    TalkService service;

    @Test
    public void testList() throws Exception {
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(new Talk("1", "Titre", "Description")));

        given()
          .when().get("/talks")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].id", is("1"))
             .body("[0].title", is("Titre"));
    }

    @Test
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given()
          .when().get("/talks/999")
          .then()
             .statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        Talk input = new Talk(null, "New Talk", "Desc");
        Talk created = new Talk("new-id", "New Talk", "Desc");
        
        Mockito.when(service.create(Mockito.any(Talk.class))).thenReturn(created);

        given()
          .contentType(ContentType.JSON)
          .body(input)
          .when().post("/talks")
          .then()
             .statusCode(201)
             .body("id", is("new-id"))
             .body("title", is("New Talk"));
    }

    @Test
    public void testCreateWithStructuredSpeakers() throws Exception {
        Talk created = new Talk("new-id", "New Talk", "Desc");
        Mockito.when(service.create(Mockito.any(Talk.class))).thenReturn(created);

        String payload = "{\"title\":\"New Talk\",\"description\":\"Desc\","
                + "\"speakers\":[{\"name\":\"Jane Doe\",\"email\":\"jane@zenika.com\"}]}";

        given()
          .contentType(ContentType.JSON)
          .body(payload)
          .when().post("/talks")
          .then()
             .statusCode(201);

        ArgumentCaptor<Talk> captor = ArgumentCaptor.forClass(Talk.class);
        Mockito.verify(service).create(captor.capture());
        assertEquals(1, captor.getValue().getSpeakers().size());
        assertEquals("Jane Doe", captor.getValue().getSpeakers().get(0).getName());
        assertEquals("jane@zenika.com", captor.getValue().getSpeakers().get(0).getEmail());
    }

    @Test
    public void testCreateIgnoresRoleInjectedInSpeakers() throws Exception {
        Talk created = new Talk("new-id", "New Talk", "Desc");
        Mockito.when(service.create(Mockito.any(Talk.class))).thenReturn(created);

        String payload = "{\"title\":\"New Talk\",\"description\":\"Desc\","
                + "\"speakers\":[{\"name\":\"Intrus\",\"email\":\"intrus@evil.com\",\"role\":\"admin\"}]}";

        given()
          .contentType(ContentType.JSON)
          .body(payload)
          .when().post("/talks")
          .then()
             .statusCode(201);

        ArgumentCaptor<Talk> captor = ArgumentCaptor.forClass(Talk.class);
        Mockito.verify(service).create(captor.capture());
        assertNull(captor.getValue().getSpeakers().get(0).getRole(),
                "Le rôle ne doit pas pouvoir être injecté depuis un payload client");
    }

    @Test
    public void testCreateRejectsUnstructuredSpeakers() throws Exception {
        // Le contrat OpenAPI annonce des objets : une chaîne doit être refusée proprement en 400,
        // pas absorbée en silence ni transformée en 500.
        String payload = "{\"title\":\"New Talk\",\"description\":\"Desc\","
                + "\"speakers\":[\"Jane Doe\"]}";

        given()
          .contentType(ContentType.JSON)
          .body(payload)
          .when().post("/talks")
          .then()
             .statusCode(400);

        Mockito.verify(service, Mockito.never()).create(Mockito.any(Talk.class));
    }

    @Test
    public void testGetTalksDoesNotExposeSpeakerRole() throws Exception {
        Talk talk = new Talk("1", "Titre", "Description");
        talk.setSpeakers(java.util.List.of(
                com.zenika.thezaurus.model.User.builder()
                        .name("Jane").email("jane@zenika.com").role("admin").build()));
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(talk));

        given()
          .when().get("/talks")
          .then()
             .statusCode(200)
             .body("[0].speakers[0].name", is("Jane"))
             .body("[0].speakers[0].role", is(nullValue()));
    }
}
