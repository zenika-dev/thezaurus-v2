package com.zenika.thezaurus.resource;

import com.zenika.thezaurus.service.TalkFeedbackReminderService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * Déclenchement du batch de rappels post-talk par Cloud Scheduler.
 *
 * <p>Le job Cloud Scheduler appelle cet endpoint avec un token OIDC signé par Google pour son
 * compte de service. Le token est validé par smallrye-jwt (signature, issuer, audience, cf.
 * {@code mp.jwt.verify.audiences}) ; on vérifie ici que l'appelant est bien ce compte de service.
 */
@Path("/api/internal/feedback-reminders")
@Authenticated
public class FeedbackReminderResource {

    @Inject
    TalkFeedbackReminderService reminders;

    @Inject
    SecurityIdentity identity;

    @ConfigProperty(name = "thezaurus.feedback-reminder.invoker")
    Optional<String> invoker;

    @POST
    @Operation(hidden = true)
    public void trigger() throws ExecutionException {
        if (!isScheduler()) {
            throw new ForbiddenException();
        }
        reminders.sendReminders();
    }

    private boolean isScheduler() {
        if (invoker.isEmpty() || invoker.get().isBlank()) return false;
        if (!(identity.getPrincipal() instanceof JsonWebToken jwt)) return false;
        String email = jwt.getClaim("email");
        Object verified = jwt.getClaim("email_verified");
        return email != null
                && Boolean.parseBoolean(String.valueOf(verified))
                && email.toLowerCase(Locale.ROOT).equals(invoker.get().trim().toLowerCase(Locale.ROOT));
    }
}
