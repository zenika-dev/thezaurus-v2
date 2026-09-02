package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zenika.thezaurus.exception.TalkReviewException;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.service.TalkReviewService;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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

    @InjectMock
    TalkReviewService talkReviewService;

    @Test
    @DisplayName("GET /talks - retourne la liste des talks")
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
    @DisplayName("GET /talks/{id} - talk non trouvé - retourne HTTP 404")
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given().when().get("/talks/999").then().statusCode(404);
    }

    @Test
    @DisplayName("POST /talks - création de talk - retourne HTTP 201")
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

    @Test
    @DisplayName("POST /talks/review - succès de la relecture IA par TalkReviewService - retourne HTTP 200")
    public void testReviewTalkSuccess() {
        TalkReviewRequest request = new TalkReviewRequest("Mets du Front dans ton back end", "Abstract du talk");
        TalkReviewResponse mockResponse = new TalkReviewResponse(
                List.of("Quarkus et Front-end Unis"),
                List.of("Abstract retravaillé"),
                List.of("[Titre] Très bon titre"),
                List.of("Point fort 1"));

        Mockito.when(talkReviewService.reviewTalk(Mockito.any(TalkReviewRequest.class)))
                .thenReturn(mockResponse);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/talks/review")
                .then()
                .statusCode(200)
                .body("suggestedTitles[0]", is("Quarkus et Front-end Unis"))
                .body("suggestedAbstracts[0]", is("Abstract retravaillé"))
                .body("feedback[0]", is("[Titre] Très bon titre"))
                .body("keyImprovements[0]", is("Point fort 1"));
    }

    @Test
    @DisplayName("POST /talks/review - requête sans titre - retourne HTTP 400 Bad Request")
    public void testReviewTalkBadRequestMissingTitle() {
        given().contentType(ContentType.JSON)
                .body("{\"abstractText\":\"Abstract sans titre\"}")
                .when()
                .post("/talks/review")
                .then()
                .statusCode(400)
                .body("error", is("Le titre et l'abstract sont requis"));
    }

    @Test
    @DisplayName(
            "POST /talks/review - exception de TalkReviewService - retourne HTTP 502 Bad Gateway via ExceptionMapper")
    public void testReviewTalkServiceException() {
        TalkReviewRequest request = new TalkReviewRequest("Titre Erreur", "Abstract Erreur");

        Mockito.when(talkReviewService.reviewTalk(Mockito.any(TalkReviewRequest.class)))
                .thenThrow(new TalkReviewException("Reasoning Engine AI Agent indisponible"));

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/talks/review")
                .then()
                .statusCode(502)
                .body("message", is("Reasoning Engine AI Agent indisponible"));
    }
}
