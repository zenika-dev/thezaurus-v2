package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class ProfileResourceTest {

    @InjectMock
    UserRepository userRepository;

    private User jane(String slackUserId, boolean emailEnabled, boolean slackEnabled) {
        return User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .slackUserId(slackUserId)
                .roles(List.of(Role.CONSULTANT))
                .emailNotificationsEnabled(emailEnabled)
                .slackNotificationsEnabled(slackEnabled)
                .build();
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testGetProfileExposesIdentityAndPreferences() throws Exception {
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane("U123", true, false));

        given().when()
                .get("/api/me/profile")
                .then()
                .statusCode(200)
                .body("name", is("Jane Doe"))
                .body("email", is("jane@zenika.com"))
                .body("notificationPreferences.email", is(true))
                .body("notificationPreferences.slack", is(false))
                .body("slackLinked", is(true));
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testGetProfileNeverExposesSlackUserIdNorRoles() throws Exception {
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane("U123", true, true));

        // Seul le booléen de rattachement est publié.
        given().when()
                .get("/api/me/profile")
                .then()
                .statusCode(200)
                .body("slackUserId", is(nullValue()))
                .body("roles", is(nullValue()));
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testGetProfileReportsSlackNotLinkedWhenSlackUserIdIsAbsent() throws Exception {
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane(null, false, false));

        given().when().get("/api/me/profile").then().statusCode(200).body("slackLinked", is(false));
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testPreferencesDefaultToDisabledWhenAbsentFromDocument() throws Exception {
        // Un document antérieur à la fonctionnalité n'a pas les champs.
        User legacy = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(legacy);

        given().when()
                .get("/api/me/profile")
                .then()
                .statusCode(200)
                .body("notificationPreferences.email", is(false))
                .body("notificationPreferences.slack", is(false));
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testUnexpressedPreferenceStaysNullOnTheModel() throws Exception {
        // null (jamais exprimée) reste distinct de false (refusée) : c'est l'API qui applique le
        // défaut, pas le record.
        User legacy = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .roles(List.of(Role.CONSULTANT))
                .build();
        assertNull(legacy.emailNotificationsEnabled());
        assertNull(legacy.slackNotificationsEnabled());
        assertFalse(legacy.notifiesByEmail());
        assertFalse(legacy.notifiesOnSlack());
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testUpdatePreferencesWritesBothChannels() throws Exception {
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane("U123", false, false));

        given().contentType(ContentType.JSON)
                .body("{\"email\":true,\"slack\":true}")
                .when()
                .put("/api/me/profile/notification-preferences")
                .then()
                .statusCode(200)
                .body("email", is(true))
                .body("slack", is(true));

        Mockito.verify(userRepository).updateNotificationPreferences("jane@zenika.com", true, true);
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testUpdatePreferencesNeverRewritesTheWholeUser() throws Exception {
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(jane("U123", false, false));

        given().contentType(ContentType.JSON)
                .body("{\"email\":true,\"slack\":false}")
                .when()
                .put("/api/me/profile/notification-preferences")
                .then()
                .statusCode(200);

        // create/update écrasent le document entier : ce chemin ne doit jamais les emprunter.
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
        Mockito.verify(userRepository, Mockito.never()).update(Mockito.anyString(), Mockito.any());
    }

    @Test
    @TestSecurity(
            user = "ghost@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testUpdatePreferencesOnDeletedAccountDoesNotRecreateIt() throws Exception {
        Mockito.when(userRepository.findByEmail("ghost@zenika.com")).thenReturn(null);

        given().contentType(ContentType.JSON)
                .body("{\"email\":true,\"slack\":true}")
                .when()
                .put("/api/me/profile/notification-preferences")
                .then()
                .statusCode(404);

        Mockito.verify(userRepository, Mockito.never())
                .updateNotificationPreferences(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.verify(userRepository, Mockito.never()).create(Mockito.any());
    }

    @Test
    public void testProfileUnauthorizedWhenAnonymous() {
        given().when().get("/api/me/profile").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "intru@evil.com",
            roles = {})
    public void testProfileForbiddenWithoutRole() {
        given().when().get("/api/me/profile").then().statusCode(403);
    }
}
