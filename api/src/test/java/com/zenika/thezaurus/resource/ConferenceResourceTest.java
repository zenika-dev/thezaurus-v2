package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.ConferenceReach;
import com.zenika.thezaurus.model.ConferenceType;
import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.ConferenceService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@QuarkusTest
@TestSecurity(
        user = "dev@zenika.com",
        roles = {Role.Names.CONSULTANT})
public class ConferenceResourceTest {

    @InjectMock
    ConferenceService service;

    @Test
    public void testList() throws Exception {
        Mockito.when(service.findAll())
                .thenReturn(Collections.singletonList(new Conference("1", "Titre", "Description")));

        given().when()
                .get("/conferences")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is("1"))
                .body("[0].name", is("Titre"));
    }

    @Test
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given().when().get("/conferences/999").then().statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        Conference input = new Conference(null, "New Conf", "Desc");
        input.setType(ConferenceType.TECHNIQUE);
        input.setReach(ConferenceReach.NATIONALE);
        Conference created = new Conference("new-id", "New Conf", "Desc");
        created.setType(ConferenceType.TECHNIQUE);
        created.setReach(ConferenceReach.NATIONALE);

        Mockito.when(service.create(Mockito.any(Conference.class))).thenReturn(created);

        given().contentType(ContentType.JSON)
                .body(input)
                .when()
                .post("/conferences")
                .then()
                .statusCode(201)
                .body("id", is("new-id"))
                .body("name", is("New Conf"));
    }

    @Test
    public void testCreateWithMissingType() throws Exception {
        String body = """
            {"name":"New Conf","reach":"Nationale"}
            """;

        given().contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/conferences")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(service);
    }

    @Test
    public void testCreateWithInvalidType() throws Exception {
        String body = """
            {"name":"New Conf","type":"Nonsense","reach":"Nationale"}
            """;

        given().contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/conferences")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(service);
    }

    @Test
    public void testCreateWithValidTypeAndReach() throws Exception {
        String body = """
            {"name":"New Conf","type":"Marketing / business","reach":"Régionale"}
            """;

        ArgumentCaptor<Conference> captor = ArgumentCaptor.forClass(Conference.class);
        Mockito.when(service.create(captor.capture())).thenAnswer(invocation -> {
            Conference c = invocation.getArgument(0);
            c.setId("new-id");
            return c;
        });

        given().contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/conferences")
                .then()
                .statusCode(201)
                .body("id", is("new-id"))
                .body("type", is("Marketing / business"))
                .body("reach", is("Régionale"));

        assertEquals(ConferenceType.MARKETING_BUSINESS, captor.getValue().getType());
        assertEquals(ConferenceReach.REGIONALE, captor.getValue().getReach());
    }
}
