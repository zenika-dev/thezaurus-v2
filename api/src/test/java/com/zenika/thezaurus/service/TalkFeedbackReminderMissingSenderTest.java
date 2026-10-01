package com.zenika.thezaurus.service;

import static org.mockito.Mockito.verifyNoInteractions;

import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(TalkFeedbackReminderMissingSenderTest.Profile.class)
class TalkFeedbackReminderMissingSenderTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("thezaurus.feedback-reminder.enabled", "true", "quarkus.mailer.from", "");
        }
    }

    @Inject
    TalkFeedbackReminderService service;

    @InjectMock
    TalkRepository talks;

    @InjectMock
    ReminderTemplateRepository templates;

    @Test
    void missingSmtpSenderDisablesRemindersWithoutPreventingStartup() throws Exception {
        service.sendReminders();

        verifyNoInteractions(talks, templates);
    }
}
