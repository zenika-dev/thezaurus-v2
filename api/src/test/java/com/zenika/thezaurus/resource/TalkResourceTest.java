package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import com.zenika.thezaurus.service.ReasoningEngineService;
import com.zenika.thezaurus.service.TalkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class TalkResourceTest {

    @InjectMock
    TalkService service;

    @InjectMock
    ReasoningEngineService reasoningEngineService;

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
    public void testReviewEndpointSuccess() throws Exception {
        TalkReviewRequest request = new TalkReviewRequest("Mets du Front dans ton back", "Abstract de présentation");
        TalkReviewResponse mockResponse = new TalkReviewResponse(
                List.of("Titre Optimisé 1", "Titre Optimisé 2"),
                List.of("Abstract Optimisé 1"),
                List.of("[Titre] Très accrocheur."),
                List.of("Conserver la promesse du titre.")
        );

        Mockito.when(reasoningEngineService.reviewTalk(Mockito.any(TalkReviewRequest.class)))
                .thenReturn(mockResponse);

        given()
          .contentType(ContentType.JSON)
          .body(request)
          .when().post("/talks/review")
          .then()
             .statusCode(200)
             .body("suggestedTitles[0]", is("Titre Optimisé 1"))
             .body("suggestedTitles[1]", is("Titre Optimisé 2"))
             .body("suggestedAbstracts[0]", is("Abstract Optimisé 1"))
             .body("feedback", hasItem("[Titre] Très accrocheur."))
             .body("keyImprovements", hasItem("Conserver la promesse du titre."));
    }

    @Test
    public void testReviewEndpointBadRequest() throws Exception {
        TalkReviewRequest invalidRequest = new TalkReviewRequest(null, "Abstract sans titre");

        given()
          .contentType(ContentType.JSON)
          .body(invalidRequest)
          .when().post("/talks/review")
          .then()
             .statusCode(400);
    }
}
