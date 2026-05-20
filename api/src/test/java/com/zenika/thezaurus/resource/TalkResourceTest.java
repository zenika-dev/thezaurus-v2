package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

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
}
