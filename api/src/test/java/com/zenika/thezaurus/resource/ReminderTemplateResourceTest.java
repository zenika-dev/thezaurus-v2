package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.*;
import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReminderTemplateResourceTest {
    @InjectMock
    ReminderTemplateRepository repository;

    @InjectMock
    TalkService talks;

    static final String PATH = "/api/admin/reminder-template";

    private Map<String, Object> update() {
        return Map.of("subject", "Sujet", "bodyHtml", "<p>{talkTitle}</p>", "revision", 0);
    }

    @Test
    void anonymousCannotReadSaveOrPreview() {
        given().get(PATH).then().statusCode(401);
        given().contentType(ContentType.JSON).body(update()).put(PATH).then().statusCode(401);
        given().contentType(ContentType.JSON)
                .body(update())
                .post(PATH + "/preview")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {Role.Names.CONSULTANT})
    void nonAdminCannotReadSaveOrPreview() {
        given().get(PATH).then().statusCode(403);
        given().contentType(ContentType.JSON).body(update()).put(PATH).then().statusCode(403);
        given().contentType(ContentType.JSON)
                .body(update())
                .post(PATH + "/preview")
                .then()
                .statusCode(403);
        verifyNoInteractions(repository, talks);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {Role.Names.ADMIN})
    void adminReadsAndSavesTemplate() throws Exception {
        when(repository.get()).thenReturn(new ReminderTemplateView("", "", 0));
        given().get(PATH).then().statusCode(200).body("subject", is("")).body("revision", is(0));
        when(repository.save("Sujet", "<p>{talkTitle}</p>", 0))
                .thenReturn(new ReminderTemplateView("Sujet", "<p>{talkTitle}</p>", 1));
        given().contentType(ContentType.JSON)
                .body(update())
                .put(PATH)
                .then()
                .statusCode(200)
                .body("revision", is(1));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {Role.Names.ADMIN})
    void conflictAndValidationHaveExplicitMessages() throws Exception {
        when(repository.save(anyString(), anyString(), anyLong()))
                .thenThrow(new ThezaurusException("Version modifiée", Response.Status.CONFLICT));
        given().contentType(ContentType.JSON)
                .body(update())
                .put(PATH)
                .then()
                .statusCode(409)
                .body("message", is("Version modifiée"));
        given().contentType(ContentType.JSON)
                .body(Map.of("subject", "Sujet", "bodyHtml", "{#if hasDate}{unknown}{/if}", "revision", 0))
                .put(PATH)
                .then()
                .statusCode(400)
                .body("message", containsString("unknown"));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {Role.Names.ADMIN})
    void previewUsesUnsavedContentWithoutPersistence() throws Exception {
        when(talks.findById("one")).thenReturn(new Talk("one", "Mon talk", "description"));
        var request = Map.of("subject", "Rappel {talkTitle}", "bodyHtml", "<p>{talkTitle}</p>", "talkId", "one");
        given().contentType(ContentType.JSON)
                .body(request)
                .post(PATH + "/preview")
                .then()
                .statusCode(200)
                .body("subject", is("Rappel Mon talk"))
                .body("bodyHtml", is("<p>Mon talk</p>"));
        verifyNoInteractions(repository);
        given().contentType(ContentType.JSON)
                .body(Map.of("subject", "Sujet", "bodyHtml", "Corps", "talkId", "absent"))
                .post(PATH + "/preview")
                .then()
                .statusCode(404);
    }
}
