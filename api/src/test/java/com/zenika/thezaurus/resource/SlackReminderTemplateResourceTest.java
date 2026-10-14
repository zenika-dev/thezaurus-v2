package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.*;
import com.zenika.thezaurus.repository.SlackReminderTemplateRepository;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SlackReminderTemplateResourceTest {
    @InjectMock
    SlackReminderTemplateRepository repository;

    @InjectMock
    TalkService talks;

    static final String PATH = "/api/admin/slack-reminder-template";

    private Map<String, Object> update() {
        return Map.of("bodyHtml", "Bonjour {{recipientMention}}, pense à renseigner *{{talkTitle}}*", "revision", 0);
    }

    @Test
    void anonymousCannotReadSaveOrPreview() {
        given().get(PATH + "/contexts").then().statusCode(401);
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
        given().get(PATH + "/contexts").then().statusCode(403);
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
        given().get(PATH).then().statusCode(200).body("bodyHtml", is("")).body("revision", is(0));
        when(repository.save("Bonjour {{recipientMention}}", 0))
                .thenReturn(new ReminderTemplateView("", "Bonjour {{recipientMention}}", 1));
        given().contentType(ContentType.JSON)
                .body(Map.of("bodyHtml", "Bonjour {{recipientMention}}", "revision", 0))
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
        when(repository.save(anyString(), anyLong()))
                .thenThrow(new ThezaurusException("Version modifiée", Response.Status.CONFLICT));
        given().contentType(ContentType.JSON)
                .body(update())
                .put(PATH)
                .then()
                .statusCode(409)
                .body("message", is("Version modifiée"));
        given().contentType(ContentType.JSON)
                .body(Map.of("bodyHtml", "{{#hasDate}}missing", "revision", 0))
                .put(PATH)
                .then()
                .statusCode(400)
                .body("message", containsString("Mustache"));
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {Role.Names.ADMIN})
    void previewUsesUnsavedContentWithoutPersistence() throws Exception {
        when(talks.findContextOptions(null))
                .thenReturn(
                        new TemplateContextPage(java.util.List.of(new TemplateContextOption("one", "Mon talk")), null));
        given().get(PATH + "/contexts")
                .then()
                .statusCode(200)
                .body("options[0].id", is("one"))
                .body("options[0].label", is("Mon talk"));
        when(talks.findById("one")).thenReturn(new Talk("one", "Mon talk", "description"));
        var request = Map.of("bodyHtml", "Rappel *{{talkTitle}}*", "contextId", "one");
        given().contentType(ContentType.JSON)
                .body(request)
                .post(PATH + "/preview")
                .then()
                .statusCode(200)
                .body("bodyHtml", is("Rappel *Mon talk*"));
        verifyNoInteractions(repository);
        given().contentType(ContentType.JSON)
                .body(Map.of("bodyHtml", "Corps", "contextId", "absent"))
                .post(PATH + "/preview")
                .then()
                .statusCode(404);
    }
}
