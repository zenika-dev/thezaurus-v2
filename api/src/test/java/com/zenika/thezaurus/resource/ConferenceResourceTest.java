package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.service.ConferenceService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ConferenceResourceTest {

    @InjectMock
    ConferenceService service;

    @Test
    public void testList() throws Exception {
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(new Conference("1", "Titre", "Description")));

        given()
          .when().get("/conferences")
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
          .when().get("/conferences/999")
          .then()
             .statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        Conference input = new Conference(null, "New Conf", "Desc");
        Conference created = new Conference("new-id", "New Conf", "Desc");
        
        Mockito.when(service.create(Mockito.any(Conference.class))).thenReturn(created);

        given()
          .contentType(ContentType.JSON)
          .body(input)
          .when().post("/conferences")
          .then()
             .statusCode(201)
             .body("id", is("new-id"))
             .body("title", is("New Conf"));
    }
}
