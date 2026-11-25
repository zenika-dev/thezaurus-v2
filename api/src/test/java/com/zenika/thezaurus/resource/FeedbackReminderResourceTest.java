package com.zenika.thezaurus.resource;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.Role;
import com.zenika.thezaurus.service.TalkFeedbackReminderService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FeedbackReminderResourceTest {
    static final String PATH = "/api/internal/feedback-reminders";

    @InjectMock
    TalkFeedbackReminderService reminders;

    @Test
    void anonymousCannotTriggerReminders() {
        given().post(PATH).then().statusCode(401);
        verifyNoInteractions(reminders);
    }

    @Test
    @TestSecurity(
            user = "admin@zenika.com",
            roles = {Role.Names.ADMIN})
    void regularUsersCannotTriggerReminders() {
        given().post(PATH).then().statusCode(403);
        verifyNoInteractions(reminders);
    }
}
