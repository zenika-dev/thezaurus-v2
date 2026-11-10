package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.service.TalkFeedbackReminderService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(FeedbackReminderLocalTriggerTest.Profile.class)
class FeedbackReminderLocalTriggerTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("thezaurus.feedback-reminder.local-trigger", "true");
        }
    }

    @InjectMock
    TalkFeedbackReminderService reminders;

    @Test
    void localTriggerAllowsAnonymousCalls() throws Exception {
        given().post(FeedbackReminderResourceTest.PATH).then().statusCode(204);
        verify(reminders).sendReminders();
    }
}
