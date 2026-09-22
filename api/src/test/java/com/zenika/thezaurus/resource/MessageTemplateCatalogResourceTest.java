package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;

import com.zenika.thezaurus.model.Role;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MessageTemplateCatalogResourceTest {
    private static final String PATH = "/api/admin/message-templates";

    @Test
    void anonymousCannotList() {
        given().get(PATH).then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "user",
            roles = {Role.Names.CONSULTANT})
    void nonAdminCannotList() {
        given().get(PATH).then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {Role.Names.ADMIN})
    void catalogExposesCurrentTemplateAndEditorMetadata() {
        given().get(PATH)
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", is("talk-reminder"))
                .body("[0].previewContext.optionsPath", is("/api/admin/reminder-template/contexts"))
                .body("[0].previewContext.label", is("Talk utilisé pour l’aperçu"))
                .body("[0].label", is("Email de rappel"))
                .body("[0].apiPath", is("/api/admin/reminder-template"))
                .body("[0].variables.name", hasItems("talkTitle", "talkDate", "conferenceName", "talksUrl", "speakers"))
                .body("[0].conditions.name", hasItems("hasConference", "hasDate", "missingReplay", "missingAudience"))
                .body("[0].links[0].variable", is("talksUrl"));
    }
}
