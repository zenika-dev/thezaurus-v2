package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.service.ProfilePhotoService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@TestProfile(ProfilePhotoEmulatorResourceTest.LocalProfile.class)
@EnabledIfEnvironmentVariable(named = "GCS_EMULATOR_TEST_HOST", matches = ".+")
class ProfilePhotoEmulatorResourceTest {
    public static class LocalProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.google.cloud.storage.host-override",
                    System.getenv().getOrDefault("GCS_EMULATOR_TEST_HOST", "http://localhost:4443"),
                    "thezaurus.photos.bucket",
                    "thezaurus-photos-local");
        }
    }

    @InjectMock
    UserRepository users;

    @Inject
    ProfilePhotoService photos;

    @Test
    @TestSecurity(user = "local@zenika.com", roles = Role.Names.CONSULTANT)
    void uploadsDisplaysControlsAgentAccessAndResetsUsingGcsEmulator() throws Exception {
        String email = "local@zenika.com";
        AtomicReference<User> current =
                new AtomicReference<>(User.builder().email(email).name("Local").build());
        when(users.findByEmail(email)).thenAnswer(invocation -> current.get());
        doAnswer(invocation -> {
                    current.updateAndGet(user -> user.toBuilder()
                            .customPhotoObject(invocation.getArgument(1))
                            .build());
                    return null;
                })
                .when(users)
                .updateCustomPhoto(eq(email), anyString());
        doAnswer(invocation -> {
                    current.updateAndGet(user -> user.toBuilder()
                            .aiPhotoAllowed(invocation.getArgument(1))
                            .build());
                    return null;
                })
                .when(users)
                .updateAiPhotoAllowed(eq(email), anyBoolean());

        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB), "png", output);
        given().contentType("image/png")
                .body(output.toByteArray())
                .put("/api/me/profile/photo")
                .then()
                .statusCode(204);
        try {
            byte[] displayed = given().get("/api/users/" + email + "/photo")
                    .then()
                    .statusCode(200)
                    .contentType("image/png")
                    .extract()
                    .asByteArray();
            assertEquals(4, ImageIO.read(new ByteArrayInputStream(displayed)).getWidth());
            assertThrows(ForbiddenException.class, () -> photos.getForAgent(email));
            given().contentType("application/json")
                    .body("{\"allowed\":true}")
                    .put("/api/me/profile/ai-photo")
                    .then()
                    .statusCode(204);
            assertArrayEquals(displayed, photos.getForAgent(email).bytes());
            given().contentType("application/json")
                    .body("{\"allowed\":false}")
                    .put("/api/me/profile/ai-photo")
                    .then()
                    .statusCode(204);
            assertThrows(ForbiddenException.class, () -> photos.getForAgent(email));
        } finally {
            given().delete("/api/me/profile/photo").then().statusCode(204);
        }
        given().get("/api/users/" + email + "/photo").then().statusCode(404);
    }
}
