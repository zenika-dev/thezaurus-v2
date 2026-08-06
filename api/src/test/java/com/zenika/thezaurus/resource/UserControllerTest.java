package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
public class UserControllerTest {

    @InjectMock
    UserRepository userRepository;

    @Test
    @TestSecurity(user = "jane@zenika.com", roles = "membre")
    public void testListUsersSummaryAsMembre() throws Exception {
        User jane = new User();
        jane.setName("Jane Doe");
        jane.setEmail("jane@zenika.com");
        jane.setRole("membre");

        Mockito.when(userRepository.findAll()).thenReturn(List.of(jane));

        given()
          .when().get("/api/users/summary")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].name", is("Jane Doe"))
             .body("[0].email", is("jane@zenika.com"))
             .body("[0].role", is(nullValue()));
    }

    @Test
    public void testListUsersSummaryUnauthorizedWhenAnonymous() {
        given()
          .when().get("/api/users/summary")
          .then()
             .statusCode(401);
    }

    @Test
    public void testGetCurrentUserUnauthorizedWhenAnonymous() {
        given()
          .when().get("/api/me")
          .then()
             .statusCode(401);
    }
}
