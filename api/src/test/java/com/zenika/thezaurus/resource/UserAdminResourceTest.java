package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class UserAdminResourceTest {

    @InjectMock
    UserRepository userRepository;

    private User user(String email, List<Role> roles) {
        return User.builder().email(email).roles(roles).build();
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = {Role.Names.ADMIN})
    public void testListUsersWithRolesAsAdmin() throws Exception {
        Mockito.when(userRepository.findAll(Mockito.anyInt())).thenReturn(List.of(
                user("jane@zenika.com", List.of(Role.CONSULTANT, Role.DT)),
                user("john@zenika.com", List.of(Role.CONSULTANT))));

        given()
          .when().get("/api/admin/users")
          .then()
             .statusCode(200)
             .body("size()", is(2))
             .body("[0].email", is("jane@zenika.com"))
             .body("[0].roles", hasItems(Role.Names.CONSULTANT, Role.Names.DT));
    }

    @Test
    @TestSecurity(user = "consultant@zenika.com", roles = {Role.Names.CONSULTANT})
    public void testListUsersForbiddenForConsultant() {
        given()
          .when().get("/api/admin/users")
          .then()
             .statusCode(403);
    }

    @Test
    public void testListUsersUnauthorizedWhenAnonymous() {
        given()
          .when().get("/api/admin/users")
          .then()
             .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = {Role.Names.ADMIN})
    public void testUpdateRolesAsAdmin() throws Exception {
        Mockito.when(userRepository.findByEmail("jane@zenika.com"))
                .thenReturn(user("jane@zenika.com", List.of(Role.CONSULTANT)));

        given()
          .contentType(ContentType.JSON)
          .body(Map.of("roles", List.of(Role.Names.CONSULTANT, Role.Names.DT)))
          .when().put("/api/admin/users/jane@zenika.com/roles")
          .then()
             .statusCode(200)
             .body("email", is("jane@zenika.com"))
             .body("roles", hasItems(Role.Names.CONSULTANT, Role.Names.DT));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).update(Mockito.eq("jane@zenika.com"), captor.capture());
        assertEquals(List.of(Role.CONSULTANT, Role.DT), captor.getValue().roles());
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = {Role.Names.ADMIN})
    public void testUpdateRolesRejectsRemovedMembreRole() {
        given()
          .contentType(ContentType.JSON)
          .body(Map.of("roles", List.of("membre")))
          .when().put("/api/admin/users/jane@zenika.com/roles")
          .then()
             .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = {Role.Names.ADMIN})
    public void testUpdateRolesRejectsUnknownRole() {
        given()
          .contentType(ContentType.JSON)
          .body(Map.of("roles", List.of("superadmin")))
          .when().put("/api/admin/users/jane@zenika.com/roles")
          .then()
             .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = {Role.Names.ADMIN})
    public void testUpdateRolesUnknownUserReturns404() throws Exception {
        Mockito.when(userRepository.findByEmail("ghost@zenika.com")).thenReturn(null);

        given()
          .contentType(ContentType.JSON)
          .body(Map.of("roles", List.of(Role.Names.CONSULTANT)))
          .when().put("/api/admin/users/ghost@zenika.com/roles")
          .then()
             .statusCode(404);
    }

    @Test
    @TestSecurity(user = "consultant@zenika.com", roles = {Role.Names.DT, Role.Names.CONSULTANT})
    public void testUpdateRolesForbiddenWithoutAdminRole() {
        given()
          .contentType(ContentType.JSON)
          .body(Map.of("roles", List.of(Role.Names.CONSULTANT)))
          .when().put("/api/admin/users/jane@zenika.com/roles")
          .then()
             .statusCode(403);
    }
}
