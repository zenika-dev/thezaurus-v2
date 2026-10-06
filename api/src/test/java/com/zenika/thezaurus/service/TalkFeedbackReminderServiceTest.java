package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.model.*;
import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestProfile(TalkFeedbackReminderServiceTest.Profile.class)
class TalkFeedbackReminderServiceTest {
    public static class Profile implements QuarkusTestProfile {
        @Produces
        @Alternative
        @Priority(1)
        @ApplicationScoped
        Mailer testMailer() {
            return mock(Mailer.class);
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "thezaurus.feedback-reminder.enabled", "true",
                    "quarkus.mailer.from", "thezaurus@example.com",
                    "thezaurus.public-url", "https://thezaurus.example",
                    "quarkus.scheduler.enabled", "false");
        }
    }

    @Inject
    TalkFeedbackReminderService service;

    @InjectMock
    TalkRepository talks;

    @InjectMock
    ReminderTemplateRepository templates;

    @InjectMock
    UserRepository users;

    @InjectMock
    Mailer mailer;

    @BeforeEach
    void configureTemplate() throws Exception {
        when(users.findByEmail(anyString()))
                .thenAnswer(invocation -> User.builder()
                        .email(invocation.getArgument(0))
                        .emailNotificationsEnabled(true)
                        .build());
        when(templates.get())
                .thenReturn(new ReminderTemplateView(
                        "Rappel {talkTitle}",
                        "<p>Bonjour {speakers}</p>{#if missingReplay}<p>Replay manquant</p>{/if}"
                                + "{#if missingAudience}<p>Audience manquante</p>{/if}<a href=\"{talksUrl}\">Compléter</a>",
                        1));
    }

    private Talk pastTalk() {
        return new Talk(
                "talk-1",
                "Java & Qute",
                "Description",
                List.of(
                        User.builder().name("Alice").email("alice@example.com").build(),
                        User.builder().name("Bob").email("bob@example.com").build()),
                "Lyon",
                new Conference(
                        null,
                        "Conférence",
                        ConferencePeriod.singleDay(LocalDate.now().minusDays(2).toString())),
                TalkStatus.ACCEPTED,
                Visibility.PUBLIC,
                "Conference",
                LocalDate.now().minusDays(2).toString(),
                null,
                null,
                null,
                null,
                null);
    }

    private static Talk candidate(
            TalkStatus status,
            String date,
            ConferencePeriod period,
            String replay,
            Integer audience,
            List<User> speakers,
            boolean sent) {
        return new Talk(
                "talk-1",
                "Java & Qute",
                "Description",
                speakers,
                "Lyon",
                period == null ? null : new Conference(null, "Conférence", period),
                status,
                Visibility.PUBLIC,
                "Conference",
                date,
                null,
                null,
                null,
                replay,
                audience,
                sent);
    }

    private static Stream<Arguments> ineligibleTalks() {
        String today = LocalDate.now(ZoneId.of("Europe/Paris")).toString();
        String yesterday = LocalDate.now(ZoneId.of("Europe/Paris")).minusDays(1).toString();
        String tomorrow = LocalDate.now(ZoneId.of("Europe/Paris")).plusDays(1).toString();
        List<User> speakers =
                List.of(User.builder().name("Alice").email("alice@example.com").build());
        return Stream.of(
                Arguments.of(
                        "already reminded",
                        candidate(TalkStatus.ACCEPTED, yesterday, null, null, null, speakers, true)),
                Arguments.of("today", candidate(TalkStatus.ACCEPTED, today, null, null, null, speakers, false)),
                Arguments.of(
                        "future",
                        candidate(
                                TalkStatus.DONE,
                                tomorrow,
                                ConferencePeriod.singleDay(yesterday),
                                null,
                                null,
                                speakers,
                                false)),
                Arguments.of("draft", candidate(TalkStatus.DRAFT, yesterday, null, null, null, speakers, false)),
                Arguments.of(
                        "submitted", candidate(TalkStatus.SUBMITTED, yesterday, null, null, null, speakers, false)),
                Arguments.of("rejected", candidate(TalkStatus.REJECTED, yesterday, null, null, null, speakers, false)),
                Arguments.of("planned", candidate(TalkStatus.PLANNED, yesterday, null, null, null, speakers, false)),
                Arguments.of("missing status", candidate(null, yesterday, null, null, null, speakers, false)),
                Arguments.of(
                        "complete including zero audience",
                        candidate(
                                TalkStatus.ACCEPTED,
                                yesterday,
                                null,
                                "https://example.com/replay",
                                0,
                                speakers,
                                false)),
                Arguments.of("missing date", candidate(TalkStatus.ACCEPTED, null, null, null, null, speakers, false)),
                Arguments.of(
                        "invalid talk date",
                        candidate(
                                TalkStatus.ACCEPTED,
                                "unknown",
                                ConferencePeriod.singleDay(yesterday),
                                null,
                                null,
                                speakers,
                                false)),
                Arguments.of(
                        "invalid conference date",
                        candidate(
                                TalkStatus.ACCEPTED,
                                null,
                                new ConferencePeriod("oops", "oops", DatePrecision.DAY),
                                null,
                                null,
                                speakers,
                                false)),
                Arguments.of(
                        "conference still ongoing",
                        candidate(
                                TalkStatus.ACCEPTED,
                                null,
                                new ConferencePeriod(yesterday, tomorrow, DatePrecision.DAY),
                                null,
                                null,
                                speakers,
                                false)),
                Arguments.of(
                        "reversed period",
                        candidate(
                                TalkStatus.ACCEPTED,
                                null,
                                new ConferencePeriod(today, yesterday, DatePrecision.DAY),
                                null,
                                null,
                                speakers,
                                false)),
                Arguments.of("no speakers", candidate(TalkStatus.ACCEPTED, yesterday, null, null, null, null, false)),
                Arguments.of(
                        "no email",
                        candidate(
                                TalkStatus.ACCEPTED,
                                yesterday,
                                null,
                                null,
                                null,
                                List.of(User.builder().name("Alice").email("  ").build()),
                                false)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ineligibleTalks")
    void ignoresTalksThatDoNotNeedAReminder(String reason, Talk talk) throws Exception {
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(talk));

        service.sendReminders();

        verifyNoInteractions(mailer);
        verify(talks, never()).markFeedbackReminderSent(anyString());
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                "|<p>Texte</p>",
                "Sujet|",
                "Sujet|<p><br></p>",
                "Sujet|{unknown}",
                "{#if hasConference}Sujet{/if}|<p>Bonjour</p>",
                "Sujet|{#if hasConference}<p>Date</p>{/if}"
            },
            delimiter = '|')
    void doesNotSendOrMarkWhenTheTemplateIsEmptyInvalidOrRendersEmpty(String subject, String body) throws Exception {
        when(templates.get())
                .thenReturn(new ReminderTemplateView(subject == null ? "" : subject, body == null ? "" : body, 1));
        when(talks.findFeedbackReminderCandidates(any()))
                .thenReturn(List.of(candidate(
                        TalkStatus.DONE,
                        LocalDate.now().minusDays(1).toString(),
                        null,
                        null,
                        null,
                        List.of(User.builder()
                                .name("Alice")
                                .email("alice@example.com")
                                .build()),
                        false)));

        assertDoesNotThrow(() -> service.sendReminders());

        verifyNoInteractions(mailer);
        verify(talks, never()).markFeedbackReminderSent(anyString());
    }

    @Test
    void sendsWhenOnlyAudienceIsMissing() throws Exception {
        Talk talk = candidate(
                TalkStatus.DONE,
                "2020-01-15",
                null,
                "https://example.com/replay",
                null,
                List.of(User.builder().name("Alice").email("alice@example.com").build()),
                false);
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(talk));

        service.sendReminders();

        var mail = org.mockito.ArgumentCaptor.forClass(Mail.class);
        verify(mailer).send(mail.capture());
        assertTrue(mail.getValue().getHtml().contains("Audience manquante"));
        assertFalse(mail.getValue().getHtml().contains("Replay manquant"));
        verify(talks).markFeedbackReminderSent("talk-1");
    }

    @Test
    void failedSmtpDeliveryIsRetriedLaterAndDoesNotBlockOtherTalks() throws Exception {
        Talk failed = pastTalk();
        Talk next = pastTalk().withId("talk-2");
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(failed, next));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .doNothing()
                .when(mailer)
                .send(any(Mail.class));

        assertDoesNotThrow(() -> service.sendReminders());

        verify(talks, never()).markFeedbackReminderSent("talk-1");
        verify(talks).markFeedbackReminderSent("talk-2");
        when(talks.findFeedbackReminderCandidates(any()))
                .thenReturn(List.of(failed, next.withFeedbackReminderSent(true)));
        service.sendReminders();
        verify(mailer, times(3)).send(any(Mail.class));
        verify(talks).markFeedbackReminderSent("talk-1");
        verify(talks, times(1)).markFeedbackReminderSent("talk-2");
    }

    @Test
    void persistenceFailureDoesNotBlockOtherTalks() throws Exception {
        when(talks.findFeedbackReminderCandidates(any()))
                .thenReturn(List.of(pastTalk(), pastTalk().withId("talk-2")));
        doThrow(new java.util.concurrent.ExecutionException(new IllegalStateException("Firestore unavailable")))
                .when(talks)
                .markFeedbackReminderSent("talk-1");

        assertDoesNotThrow(() -> service.sendReminders());

        verify(mailer, times(2)).send(any(Mail.class));
        verify(talks).markFeedbackReminderSent("talk-2");
    }

    @Test
    void interruptionStopsTheBatchAndPreservesTheInterruptFlag() throws Exception {
        when(talks.findFeedbackReminderCandidates(any()))
                .thenReturn(List.of(pastTalk(), pastTalk().withId("talk-2")));
        doThrow(new InterruptedException("Stopping")).when(talks).markFeedbackReminderSent("talk-1");
        try {
            assertDoesNotThrow(() -> service.sendReminders());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(mailer).send(any(Mail.class));
            verify(talks, never()).markFeedbackReminderSent("talk-2");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void collectiveDeliveryUsesCurrentEmailPreferencesRatherThanTheEmbeddedSpeaker() throws Exception {
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(pastTalk()));
        when(users.findByEmail("bob@example.com"))
                .thenReturn(User.builder()
                        .email("bob@example.com")
                        .emailNotificationsEnabled(false)
                        .build());

        service.sendReminders();

        var mail = org.mockito.ArgumentCaptor.forClass(Mail.class);
        verify(mailer).send(mail.capture());
        assertEquals(List.of("alice@example.com"), mail.getValue().getTo());
        verify(talks).markFeedbackReminderSent("talk-1");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void noEmailOptInLeavesTheTalkUnsent(Boolean preference) throws Exception {
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(pastTalk()));
        when(users.findByEmail(anyString()))
                .thenReturn(User.builder().emailNotificationsEnabled(preference).build());

        service.sendReminders();

        verifyNoInteractions(mailer);
        verify(talks, never()).markFeedbackReminderSent(anyString());
    }

    @Test
    void unknownUsersAreNotOptedInAndMayReceiveTheReminderAfterOptingIn() throws Exception {
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(pastTalk()));
        when(users.findByEmail(anyString())).thenReturn(null);
        service.sendReminders();
        verifyNoInteractions(mailer);
        verify(talks, never()).markFeedbackReminderSent(anyString());

        when(users.findByEmail("alice@example.com"))
                .thenReturn(User.builder().emailNotificationsEnabled(true).build());
        service.sendReminders();
        verify(mailer).send(any(Mail.class));
        verify(talks).markFeedbackReminderSent("talk-1");
    }

    @Test
    void sendsOneCollectiveEmailUsingTheConfiguredTemplateThenPersistsTheReminder() throws Exception {
        when(talks.findFeedbackReminderCandidates(any())).thenReturn(List.of(pastTalk()));
        doAnswer(invocation -> {
                    Mail mail = invocation.getArgument(0);
                    assertEquals(List.of("alice@example.com", "bob@example.com"), mail.getTo());
                    assertEquals("Rappel Java & Qute", mail.getSubject());
                    assertEquals(
                            "<p>Bonjour Alice, Bob</p><p>Replay manquant</p><p>Audience manquante</p>"
                                    + "<a href=\"https://thezaurus.example/talks\">Compléter</a>",
                            mail.getHtml());
                    verify(talks, never()).markFeedbackReminderSent(anyString());
                    return null;
                })
                .when(mailer)
                .send(any(Mail.class));

        service.sendReminders();

        verify(mailer).send(any(Mail.class));
        verify(talks).markFeedbackReminderSent("talk-1");
    }
}
