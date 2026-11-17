package com.zenika.thezaurus.resource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.zenika.thezaurus.service.TalkFeedbackReminderService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Autorisation de l'appel Cloud Scheduler : seul un token OIDC du compte de service configuré
 * ({@code thezaurus.feedback-reminder.invoker}), avec un email vérifié, déclenche le batch.
 */
class FeedbackReminderSchedulerAuthTest {
    static final String SCHEDULER = "thezaurus-scheduler-sa@project.iam.gserviceaccount.com";

    FeedbackReminderResource resource;
    TalkFeedbackReminderService reminders;
    SecurityIdentity identity;

    @BeforeEach
    void setUp() {
        reminders = mock(TalkFeedbackReminderService.class);
        identity = mock(SecurityIdentity.class);
        resource = new FeedbackReminderResource();
        resource.reminders = reminders;
        resource.identity = identity;
        resource.invoker = Optional.of(SCHEDULER);
        resource.localTrigger = false;
    }

    private void authenticatedAs(String email, Object emailVerified) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.<String>getClaim("email")).thenReturn(email);
        when(jwt.<Object>getClaim("email_verified")).thenReturn(emailVerified);
        when(identity.isAnonymous()).thenReturn(false);
        when(identity.getPrincipal()).thenReturn(jwt);
    }

    @Test
    void schedulerServiceAccountTriggersTheBatch() throws Exception {
        authenticatedAs(SCHEDULER, true);
        resource.trigger();
        verify(reminders).sendReminders();
    }

    @Test
    void invokerComparisonIgnoresCaseAndSurroundingSpaces() throws Exception {
        resource.invoker = Optional.of("  " + SCHEDULER.toUpperCase() + " ");
        authenticatedAs(SCHEDULER, "true");
        resource.trigger();
        verify(reminders).sendReminders();
    }

    @Test
    void unverifiedEmailIsRejected() throws Exception {
        authenticatedAs(SCHEDULER, false);
        assertThrows(ForbiddenException.class, resource::trigger);
        verify(reminders, never()).sendReminders();
    }

    @Test
    void otherServiceAccountIsRejected() throws Exception {
        authenticatedAs("other-sa@project.iam.gserviceaccount.com", true);
        assertThrows(ForbiddenException.class, resource::trigger);
        verify(reminders, never()).sendReminders();
    }

    @Test
    void missingInvokerConfigurationRejectsEveryone() throws Exception {
        resource.invoker = Optional.empty();
        authenticatedAs(SCHEDULER, true);
        assertThrows(ForbiddenException.class, resource::trigger);
        verify(reminders, never()).sendReminders();
    }

    @Test
    void anonymousCallIsUnauthorized() throws Exception {
        when(identity.isAnonymous()).thenReturn(true);
        assertThrows(NotAuthorizedException.class, resource::trigger);
        verify(reminders, never()).sendReminders();
    }
}
