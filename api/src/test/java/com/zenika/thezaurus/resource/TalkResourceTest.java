package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
@TestSecurity(
        user = "dev@zenika.com",
        roles = {Role.Names.CONSULTANT})
public class TalkResourceTest {

    @InjectMock
    TalkService service;

    @Test
    public void testList() throws Exception {
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(new Talk("1", "Titre", "Description")));

        given().when()
                .get("/talks")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is("1"))
                .body("[0].title", is("Titre"));
    }

    @Test
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given().when().get("/talks/999").then().statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        Talk input = new Talk(null, "New Talk", "Desc");
        Talk created = new Talk("new-id", "New Talk", "Desc");

        Mockito.when(service.create(Mockito.any(Talk.class))).thenReturn(created);

        given().contentType(ContentType.JSON)
                .body(input)
                .when()
                .post("/talks")
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

        given().contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/talks")
                .then()
                .statusCode(201);

        ArgumentCaptor<Talk> captor = ArgumentCaptor.forClass(Talk.class);
        Mockito.verify(service).create(captor.capture());
        assertEquals(1, captor.getValue().speakers().size());
        assertEquals("Jane Doe", captor.getValue().speakers().get(0).name());
        assertEquals("jane@zenika.com", captor.getValue().speakers().get(0).email());
    }

    @Test
    public void testCreateIgnoresRolesInjectedInSpeakers() throws Exception {
        Talk created = new Talk("new-id", "New Talk", "Desc");
        Mockito.when(service.create(Mockito.any(Talk.class))).thenReturn(created);

        String payload = "{\"title\":\"New Talk\",\"description\":\"Desc\","
                + "\"speakers\":[{\"name\":\"Intrus\",\"email\":\"intrus@evil.com\",\"roles\":[\"ADMIN\"]}]}";

        given().contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/talks")
                .then()
                .statusCode(201);

        ArgumentCaptor<Talk> captor = ArgumentCaptor.forClass(Talk.class);
        Mockito.verify(service).create(captor.capture());
        assertNull(
                captor.getValue().speakers().get(0).roles(),
                "Les rôles ne doivent pas pouvoir être injectés depuis un payload client");
    }

    @Test
    public void testCreateRejectsUnstructuredSpeakers() throws Exception {
        // Le contrat OpenAPI annonce des objets : une chaîne doit être refusée proprement en 400,
        // pas absorbée en silence ni transformée en 500.
        String payload = "{\"title\":\"New Talk\",\"description\":\"Desc\"," + "\"speakers\":[\"Jane Doe\"]}";

        given().contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/talks")
                .then()
                .statusCode(400);

        Mockito.verify(service, Mockito.never()).create(Mockito.any(Talk.class));
    }

    @Test
    public void testGetTalksDoesNotExposeSpeakerRoles() throws Exception {
        Talk talk = new Talk(
                        "Titre",
                        "Description",
                        List.of(User.builder()
                                .name("Jane")
                                .email("jane@zenika.com")
                                .roles(List.of(Role.ADMIN))
                                .build()),
                        null,
                        null,
                        null)
                .withId("1");
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(talk));

        given().when()
                .get("/talks")
                .then()
                .statusCode(200)
                .body("[0].speakers[0].name", is("Jane"))
                .body("[0].speakers[0].roles", is(nullValue()));
    }

    @Test
    public void testDeleteForbiddenForConsultant() {
        given().when().delete("/talks/1").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "admin@zenika.com",
            roles = {Role.Names.ADMIN})
    public void testDeleteAsAdmin() throws Exception {
        Mockito.when(service.delete("1")).thenReturn(true);

        given().when().delete("/talks/1").then().statusCode(204);
    }
}
