package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.slack.SlackUserResolver;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class UserControllerTest {

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    SlackUserResolver slackUserResolver;

    @Test
    @TestSecurity(
            user = "maxime.mainguet@zenika.com",
            roles = {Role.Names.CONSULTANT, Role.Names.DT})
    public void testMeReflectsAllRoles() {
        given().when()
                .get("/api/me")
                .then()
                .statusCode(200)
                .body("email", is("maxime.mainguet@zenika.com"))
                .body("roles", hasItems(Role.Names.CONSULTANT, Role.Names.DT));
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testListUsersAsConsultant() throws Exception {
        User jane = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findAll(Mockito.anyInt())).thenReturn(List.of(jane));

        given().when()
                .get("/api/users")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", is("Jane Doe"))
                .body("[0].email", is("jane@zenika.com"))
                .body("[0].roles", is(nullValue()));
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testListUsersIsBounded() throws Exception {
        Mockito.when(userRepository.findAll(Mockito.anyInt())).thenReturn(List.of());

        given().when().get("/api/users").then().statusCode(200);

        // La liste doit toujours être bornée : pas de scan complet de la collection.
        Mockito.verify(userRepository).findAll(500);
    }

    @Test
    @TestSecurity(
            user = "intru@evil.com",
            roles = {})
    public void testListUsersForbiddenWithoutRole() {
        given().when().get("/api/users").then().statusCode(403);
    }

    @Test
    public void testListUsersUnauthorizedWhenAnonymous() {
        given().when().get("/api/users").then().statusCode(401);
    }

    @Test
    public void testGetCurrentUserUnauthorizedWhenAnonymous() {
        given().when().get("/api/me").then().statusCode(401);
    }

    // --- Rattachement Slack au login -----------------------------------------------------------
    // /api/me est appelé une fois par connexion : c'est le déclencheur du rattachement.

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testMeTriggersSlackResolutionWhenSlackUserIdIsAbsent() throws Exception {
        User jane = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane);

        given().when().get("/api/me").then().statusCode(200);

        Mockito.verify(slackUserResolver).resolveAndPersistAsync("jane@zenika.com");
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testMeDoesNotRetriggerSlackResolutionWhenAlreadyLinked() throws Exception {
        User jane = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .slackUserId("U123")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane);

        given().when().get("/api/me").then().statusCode(200);

        Mockito.verifyNoInteractions(slackUserResolver);
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testMeStillAnswersWhenSlackResolutionBlowsUp() throws Exception {
        // Enrichissement, jamais un prérequis : la connexion doit aboutir.
        User jane = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane);
        Mockito.when(slackUserResolver.resolveAndPersistAsync(Mockito.anyString()))
                .thenThrow(new RuntimeException("Slack est tombé"));

        given().when().get("/api/me").then().statusCode(200).body("email", is("jane@zenika.com"));
    }
}
