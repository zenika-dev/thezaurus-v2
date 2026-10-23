package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.service.ProfilePhotoStorage;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProfilePhotoResourceTest {
    @InjectMock
    UserRepository users;

    @InjectMock
    ProfilePhotoStorage storage;

    final String email = "jane@zenika.com";

    @BeforeEach
    void setup() throws Exception {
        when(users.findByEmail(email))
                .thenReturn(User.builder()
                        .email(email)
                        .name("Jane")
                        .customPhotoObject("old")
                        .build());
        when(users.findByEmail("admin@zenika.com"))
                .thenReturn(
                        User.builder().email("admin@zenika.com").name("Admin").build());
    }

    @Test
    void anonymousCannotChangeConsentOrReadPhoto() {
        given().contentType("application/json")
                .body("{\"allowed\":true}")
                .put("/api/me/profile/ai-photo")
                .then()
                .statusCode(401);
        given().get("/api/users/" + email + "/photo").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "jane@zenika.com", roles = Role.Names.CONSULTANT)
    void consentUsesAuthenticatedIdentityEvenIfBodyNamesAnotherUser() throws Exception {
        given().contentType("application/json")
                .body("{\"allowed\":true,\"email\":\"other@zenika.com\"}")
                .put("/api/me/profile/ai-photo")
                .then()
                .statusCode(204);
        verify(users).updateAiPhotoAllowed(email, true);
        verify(users, never()).updateAiPhotoAllowed(eq("other@zenika.com"), anyBoolean());
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = Role.Names.ADMIN)
    void adminCannotOverrideAnothersConsent() throws Exception {
        given().contentType("application/json")
                .body("{\"allowed\":true}")
                .put("/api/admin/users/" + email + "/ai-photo")
                .then()
                .statusCode(405);
        given().contentType("application/json")
                .body("{\"allowed\":true,\"email\":\"jane@zenika.com\"}")
                .put("/api/me/profile/ai-photo")
                .then()
                .statusCode(204);
        verify(users).updateAiPhotoAllowed("admin@zenika.com", true);
        verify(users, never()).updateAiPhotoAllowed(eq(email), anyBoolean());
    }

    @Test
    @TestSecurity(user = "jane@zenika.com", roles = Role.Names.CONSULTANT)
    void missingConsentIsInvalidAndResetPreservesOtherFields() throws Exception {
        given().contentType("application/json")
                .body("{}")
                .put("/api/me/profile/ai-photo")
                .then()
                .statusCode(400);
        given().delete("/api/me/profile/photo").then().statusCode(204);
        verify(users).updateCustomPhoto(email, "");
        verify(users, never()).updateAiPhotoAllowed(anyString(), anyBoolean());
    }

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT, Role.Names.DT})
    void nonAdminCannotChangeAnothersPhoto() {
        given().contentType("image/png")
                .body(new byte[] {1})
                .put("/api/admin/users/other@zenika.com/photo")
                .then()
                .statusCode(403);
        given().delete("/api/admin/users/other@zenika.com/photo").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin@zenika.com", roles = Role.Names.ADMIN)
    void adminCanUploadAndResetWithoutChangingConsent() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        when(storage.save(any(), eq("image/png"))).thenReturn("new");
        given().contentType("image/png")
                .body(out.toByteArray())
                .put("/api/admin/users/" + email + "/photo")
                .then()
                .statusCode(204);
        verify(users).updateCustomPhoto(email, "new");
        given().delete("/api/admin/users/" + email + "/photo").then().statusCode(204);
        verify(users).updateCustomPhoto(email, "");
        verify(users, never()).updateAiPhotoAllowed(anyString(), anyBoolean());
    }

    @Test
    @TestSecurity(user = "jane@zenika.com", roles = Role.Names.CONSULTANT)
    void ordinaryDisplayDoesNotRequireAiConsentAndDisablesCaching() {
        when(storage.read("old")).thenReturn(new byte[] {1, 2, 3});
        given().get("/api/users/" + email + "/photo")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .header("Cache-Control", "private, no-store");
    }
}
