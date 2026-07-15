package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.CityCount;
import com.zenika.thezaurus.model.Event;
import com.zenika.thezaurus.model.EventTypeSummary;
import com.zenika.thezaurus.model.EventsDashboard;
import com.zenika.thezaurus.model.EventsTotals;
import com.zenika.thezaurus.model.MonthLabel;
import com.zenika.thezaurus.model.MonthlyActivity;
import com.zenika.thezaurus.service.EventService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class EventResourceTest {

    @InjectMock
    EventService service;

    @Test
    public void testList() throws Exception {
        Mockito.when(service.findAll()).thenReturn(Collections.singletonList(new Event("1", "NightClazz Nantes", "NightClazz")));

        given()
          .when().get("/events")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].id", is("1"))
             .body("[0].name", is("NightClazz Nantes"));
    }

    @Test
    public void testGetNotFound() throws Exception {
        Mockito.when(service.findById("999")).thenReturn(null);

        given()
          .when().get("/events/999")
          .then()
             .statusCode(404);
    }

    @Test
    public void testCreate() throws Exception {
        Event input = new Event(null, "NightClazz Nantes", "NightClazz");
        Event created = new Event("new-id", "NightClazz Nantes", "NightClazz");

        Mockito.when(service.create(Mockito.any(Event.class))).thenReturn(created);

        given()
          .contentType(ContentType.JSON)
          .body(input)
          .when().post("/events")
          .then()
             .statusCode(201)
             .body("id", is("new-id"))
             .body("name", is("NightClazz Nantes"));
    }

    @Test
    public void testDashboard() throws Exception {
        EventsDashboard dashboard = new EventsDashboard(
                2026,
                new EventsTotals(30, 19),
                List.of(new MonthlyActivity(MonthLabel.JANUARY, 10, 0)),
                List.of(new EventTypeSummary("NightClazz", "internal", 2, List.of(new CityCount("Nantes", 1)))));

        Mockito.when(service.getDashboard(2026)).thenReturn(dashboard);

        given()
          .queryParam("year", 2026)
          .when().get("/events/dashboard")
          .then()
             .statusCode(200)
             .body("year", is(2026))
             .body("totals.internal", is(30))
             .body("totals.external", is(19))
             .body("monthly[0].month", is("Jan"))
             .body("eventTypes[0].name", is("NightClazz"))
             .body("eventTypes[0].cities[0].city", is("Nantes"));
    }
}
