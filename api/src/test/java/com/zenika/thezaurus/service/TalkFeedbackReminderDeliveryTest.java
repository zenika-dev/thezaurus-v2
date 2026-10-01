package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.*;
import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(TalkFeedbackReminderDeliveryTest.Profile.class)
class TalkFeedbackReminderDeliveryTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "thezaurus.feedback-reminder.enabled",
                    "true",
                    "quarkus.mailer.from",
                    "thezaurus@example.com",
                    "quarkus.scheduler.enabled",
                    "true",
                    "thezaurus.feedback-reminder.cron",
                    "0 0 9 1 1 ? 2099");
        }
    }

    @Inject
    TalkFeedbackReminderService service;

    @Inject
    Scheduler scheduler;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    TalkRepository talks;

    @InjectMock
    ReminderTemplateRepository templates;

    @InjectMock
    UserRepository users;

    @Test
    void registersTheConfiguredScheduleInTheParisTimeZone() {
        var trigger = scheduler.getScheduledJob("talk-feedback-reminder");
        assertNotNull(trigger);
        assertEquals(Instant.parse("2099-01-01T08:00:00Z"), trigger.getNextFireTime());
    }

    @Test
    void deliversWithTheConfiguredSenderUsingTheRealQuarkusMailer() throws Exception {
        mailbox.clear();
        when(users.findByEmail("alice@example.com"))
                .thenReturn(User.builder().emailNotificationsEnabled(true).build());
        when(templates.get())
                .thenReturn(new ReminderTemplateView("Rappel {talkTitle}", "<p>Bonjour {speakers}</p>", 1));
        when(talks.findAll())
                .thenReturn(List.of(new Talk(
                        "talk-1",
                        "Java",
                        "Description",
                        List.of(User.builder()
                                .name("Alice")
                                .email("alice@example.com")
                                .build()),
                        "Lyon",
                        null,
                        TalkStatus.DONE,
                        Visibility.PUBLIC,
                        "Conference",
                        "2020-01-01",
                        null,
                        null,
                        null,
                        null,
                        0)));

        service.sendReminders();

        var delivered = mailbox.getMailMessagesSentTo("alice@example.com");
        assertEquals(1, delivered.size());
        assertEquals("thezaurus@example.com", delivered.getFirst().getFrom());
        assertEquals("Rappel Java", delivered.getFirst().getSubject());
        assertEquals("<p>Bonjour Alice</p>", delivered.getFirst().getHtml());
        verify(talks).markFeedbackReminderSent("talk-1");
    }
}
