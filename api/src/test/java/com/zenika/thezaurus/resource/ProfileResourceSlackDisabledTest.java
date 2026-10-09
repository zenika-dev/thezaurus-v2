package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestProfile(ProfileResourceSlackDisabledTest.Profile.class)
public class ProfileResourceSlackDisabledTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("thezaurus.slack.bot-token", "");
        }
    }

    @InjectMock
    UserRepository userRepository;

    @Test
    @TestSecurity(
            user = "jane@zenika.com",
            roles = {Role.Names.CONSULTANT})
    public void testGetProfileReportsSlackNotLinkedWhenBotTokenIsBlank() throws Exception {
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@zenika.com")
                .slackUserId("U123")
                .roles(List.of(Role.CONSULTANT))
                .build();
        Mockito.when(userRepository.findByEmail("jane@zenika.com")).thenReturn(user);

        given().when().get("/api/me/profile").then().statusCode(200).body("slackAvailable", is(false));
    }
}
