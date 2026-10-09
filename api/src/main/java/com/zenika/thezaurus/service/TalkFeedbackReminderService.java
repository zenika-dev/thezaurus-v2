package com.zenika.thezaurus.service;

import com.slack.api.bolt.App;
import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.ReminderTemplateRepository;
import com.zenika.thezaurus.repository.SlackReminderTemplateRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import com.zenika.thezaurus.repository.UserRepository;
import com.zenika.thezaurus.slack.SlackUserResolver;
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
    SlackReminderTemplateRepository slackTemplates;

    @Inject
    UserRepository users;

    @Inject
    ReminderTemplateRenderer renderer;

    @Inject
    SlackReminderTemplateRenderer slackRenderer;

    @Inject
    SlackUserResolver slackUserResolver;

    @Inject
    App slackApp;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "thezaurus.feedback-reminder.time-zone", defaultValue = "Europe/Paris")
    String timeZone;

    @ConfigProperty(name = "thezaurus.feedback-reminder.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "quarkus.mailer.from")
    Optional<String> sender;

    @ConfigProperty(name = "thezaurus.slack.bot-token")
    Optional<String> botToken;

    @Scheduled(
            identity = "talk-feedback-reminder",
            cron = "{thezaurus.feedback-reminder.cron}",
            timeZone = "{thezaurus.feedback-reminder.time-zone}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void sendReminders() throws ExecutionException {
        if (!enabled) return;
        boolean hasSmtp = sender.isPresent() && !sender.get().isBlank();
        boolean hasSlack = botToken.isPresent() && !botToken.get().isBlank();
        if (!hasSmtp && !hasSlack) {
            LOG.warn("Aucun canal de notification configuré (SMTP et Slack absents) : aucun rappel envoyé.");
            return;
        }
        try {
            sendBatch(hasSmtp, hasSlack);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOG.warn("Envoi des rappels interrompu.");
        } catch (Exception error) {
            LOG.errorv(error, "Erreur lors de l'exécution du batch de rappels post-talk.");
        }
    }

    private void sendBatch(boolean hasSmtp, boolean hasSlack) throws ExecutionException, InterruptedException {
        var emailTemplate = hasSmtp ? templates.get() : null;
        boolean emailTemplateValid = false;
        if (emailTemplate != null
                && !emailTemplate.subject().isBlank()
                && !emailTemplate.bodyHtml().isBlank()) {
            try {
                renderer.validate(emailTemplate.subject(), emailTemplate.bodyHtml());
                emailTemplateValid = true;
            } catch (ThezaurusException invalidTemplate) {
                LOG.warn("Modèle d'email de rappel invalide.");
            }
        }

        var slackTemplate = hasSlack ? slackTemplates.get() : null;
        boolean slackTemplateValid = false;
        if (slackTemplate != null && !slackTemplate.bodyHtml().isBlank()) {
            try {
                slackRenderer.validate(slackTemplate.bodyHtml());
                slackTemplateValid = true;
            } catch (ThezaurusException invalidTemplate) {
                LOG.warn("Modèle Slack de rappel invalide.");
            }
        }

        if (!emailTemplateValid && !slackTemplateValid) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        var candidates = talks.findFeedbackReminderCandidates(today);
        if (candidates.isEmpty()) return;

        java.util.Set<String> allEmails = new java.util.HashSet<>();
        for (Talk talk : candidates) {
            if (talk.speakers() != null) {
                for (com.zenika.thezaurus.model.User speaker : talk.speakers()) {
                    if (speaker != null
                            && speaker.email() != null
                            && !speaker.email().isBlank()) {
                        allEmails.add(speaker.email().trim().toLowerCase());
                    }
                }
            }
        }
        var userMap = users.findByEmails(allEmails);

        for (Talk talk : candidates) {
            if (!eligible(talk, today)) continue;
            boolean anySuccess = false;

            // Envoi par email
            if (emailTemplateValid) {
                try {
                    var message = renderer.render(emailTemplate.subject(), emailTemplate.bodyHtml(), talk);
                    if (!message.to().isEmpty()
                            && !message.subject().isBlank()
                            && !Jsoup.parseBodyFragment(message.bodyHtml())
                                    .text()
                                    .replace('\u00a0', ' ')
                                    .isBlank()) {
                        var recipients = new ArrayList<String>();
                        for (String email : message.to()) {
                            var user = userMap.get(email.trim().toLowerCase());
                            if (user == null) {
                                user = users.findByEmail(email);
                            }
                            if (user != null && user.notifiesByEmail()) recipients.add(email);
                        }
                        if (!recipients.isEmpty()) {
                            mailer.send(new Mail()
                                    .setSubject(message.subject())
                                    .setHtml(message.bodyHtml())
                                    .setTo(recipients));
                            LOG.infof("Email de rappel envoyé avec succès à %s pour le talk %s", recipients, talk.id());
                            anySuccess = true;
                        }
                    }
                } catch (Exception failure) {
                    LOG.errorv(failure, "Échec de l'envoi d'email de rappel pour le talk {0}", talk.id());
                }
            }

            // Envoi par Slack
            if (slackTemplateValid && talk.speakers() != null) {
                for (User speaker : talk.speakers()) {
                    if (speaker == null
                            || speaker.email() == null
                            || speaker.email().isBlank()) continue;
                    try {
                        var user = userMap.get(speaker.email().trim().toLowerCase());
                        if (user == null) {
                            user = users.findByEmail(speaker.email());
                        }
                        if (user != null && user.notifiesOnSlack()) {
                            String slackUserId = user.slackUserId();
                            if (slackUserId == null || slackUserId.isBlank()) {
                                slackUserId = slackUserResolver
                                        .resolveAndPersist(user.email())
                                        .orElse(null);
                                if (slackUserId != null) {
                                    user = user.withSlackUserId(slackUserId);
                                }
                            }
                            if (slackUserId != null && !slackUserId.isBlank()) {
                                String slackText = slackRenderer.render(slackTemplate.bodyHtml(), talk, user);
                                if (!slackText.isBlank()) {
                                    String finalSlackId = slackUserId;
                                    var response = slackApp.client()
                                            .chatPostMessage(r -> r.token(botToken.get())
                                                    .channel(finalSlackId)
                                                    .text(slackText));
                                    if (response != null && response.isOk()) {
                                        LOG.infof(
                                                "Message Slack de rappel envoyé avec succès à %s (%s) pour le talk %s",
                                                user.email(), finalSlackId, talk.id());
                                        anySuccess = true;
                                    } else {
                                        LOG.warnf(
                                                "Erreur lors de l'envoi Slack au speaker %s pour le talk %s: error=%s, needed=%s, provided=%s",
                                                user.email(),
                                                talk.id(),
                                                response == null ? "null" : response.getError(),
                                                response == null ? "null" : response.getNeeded(),
                                                response == null ? "null" : response.getProvided());
                                    }
                                }
                            }
                        }
                    } catch (Exception failure) {
                        LOG.errorv(
                                failure, "Échec de l'envoi Slack pour {0} sur le talk {1}", speaker.email(), talk.id());
                    }
                }
            }

            if (anySuccess) {
                try {
                    talks.markFeedbackReminderSent(talk.id());
                } catch (ExecutionException failure) {
                    LOG.errorv(failure, "Échec de l'enregistrement de l'envoi du rappel pour le talk {0}", talk.id());
                }
            }
        }
    }

    private String effectiveDate(Talk talk) {
        if (talk.date() != null && !talk.date().isBlank()) {
            return talk.date().trim();
        }
        if (talk.conference() != null && talk.conference().getDate() != null) {
            String end = talk.conference().getDate().getEnd();
            String start = talk.conference().getDate().getStart();
            if (end != null && !end.isBlank()) {
                if (start != null && !start.isBlank()) {
                    try {
                        LocalDate startDate = LocalDate.parse(start.trim());
                        LocalDate endDate = LocalDate.parse(end.trim());
                        if (startDate.isAfter(endDate)) {
                            return null;
                        }
                    } catch (DateTimeParseException ignored) {
                        return null;
                    }
                }
                return end.trim();
            }
            if (start != null && !start.isBlank()) {
                return start.trim();
            }
        }
        return null;
    }

    private boolean eligible(Talk talk, LocalDate today) {
        if (talk.feedbackReminderSent()
                || (talk.status() != TalkStatus.ACCEPTED && talk.status() != TalkStatus.DONE)
                || (talk.replay() != null && !talk.replay().isBlank() && talk.audience() != null)) return false;
        String dateStr = effectiveDate(talk);
        if (dateStr == null) {
            return false;
        }
        try {
            return LocalDate.parse(dateStr).isBefore(today);
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }
}
