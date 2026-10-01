package com.zenika.thezaurus.service;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import com.zenika.thezaurus.repository.UserRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;

@ApplicationScoped
public class TalkFeedbackReminderService {
    private static final Logger LOG = Logger.getLogger(TalkFeedbackReminderService.class);

    @Inject
    TalkRepository talks;

    @Inject
    ReminderTemplateRepository templates;

    @Inject
    UserRepository users;

    @Inject
    ReminderTemplateRenderer renderer;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "thezaurus.feedback-reminder.time-zone", defaultValue = "Europe/Paris")
    String timeZone;

    @ConfigProperty(name = "thezaurus.feedback-reminder.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "quarkus.mailer.from")
    Optional<String> sender;

    @Scheduled(
            identity = "talk-feedback-reminder",
            cron = "${thezaurus.feedback-reminder.cron}",
            timeZone = "${thezaurus.feedback-reminder.time-zone}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void sendReminders() throws ExecutionException {
        if (!enabled) return;
        if (sender.isEmpty() || sender.get().isBlank()) {
            LOG.warn("Expéditeur SMTP absent : aucun rappel envoyé.");
            return;
        }
        try {
            sendBatch();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOG.warn("Envoi des rappels interrompu.");
        }
    }

    private void sendBatch() throws ExecutionException, InterruptedException {
        var template = templates.get();
        if (template.subject().isBlank() || template.bodyHtml().isBlank()) return;
        try {
            renderer.validate(template.subject(), template.bodyHtml());
        } catch (ThezaurusException invalidTemplate) {
            LOG.warn("Modèle de rappel invalide : aucun rappel envoyé.");
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        for (Talk talk : talks.findAll()) {
            if (!eligible(talk, today)) continue;
            try {
                var message = renderer.render(template.subject(), template.bodyHtml(), talk);
                if (message.to().isEmpty()
                        || message.subject().isBlank()
                        || Jsoup.parseBodyFragment(message.bodyHtml())
                                .text()
                                .replace('\u00a0', ' ')
                                .isBlank()) continue;
                var recipients = new ArrayList<String>();
                for (String email : message.to()) {
                    var user = users.findByEmail(email);
                    if (user != null && user.notifiesByEmail()) recipients.add(email);
                }
                if (recipients.isEmpty()) continue;
                mailer.send(new Mail()
                        .setSubject(message.subject())
                        .setHtml(message.bodyHtml())
                        .setTo(recipients));
                talks.markFeedbackReminderSent(talk.id());
            } catch (ExecutionException | RuntimeException failure) {
                LOG.errorv(failure, "Échec du rappel pour le talk {0} ; poursuite des autres rappels.", talk.id());
            }
        }
    }

    private boolean eligible(Talk talk, LocalDate today) {
        if (talk.feedbackReminderSent()
                || (talk.status() != TalkStatus.ACCEPTED && talk.status() != TalkStatus.DONE)
                || (talk.replay() != null && !talk.replay().isBlank() && talk.audience() != null)) return false;
        try {
            if (talk.date() != null && !talk.date().isBlank()) {
                return LocalDate.parse(talk.date().trim()).isBefore(today);
            }
            if (talk.conference() == null || talk.conference().getDate() == null) return false;
            var period = talk.conference().getDate();
            if (period.getStart() == null || period.getEnd() == null) return false;
            LocalDate start = LocalDate.parse(period.getStart().trim());
            LocalDate end = LocalDate.parse(period.getEnd().trim());
            return !start.isAfter(end) && end.isBefore(today);
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }
}
