package com.zenika.thezaurus.service;

import com.zenika.thezaurus.client.MailgunClient;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Choisit le transport des emails de rappel : API Mailgun si elle est configurée (prod),
 * sinon quarkus-mailer en SMTP (Mailpit en local, mock en test).
 */
@ApplicationScoped
public class ReminderMailSender {

    @Inject
    MailgunClient mailgun;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "quarkus.mailer.from")
    Optional<String> sender;

    public String transport() {
        return mailgun.isConfigured() ? "mailgun (" + mailgun.domain() + ")" : "smtp";
    }

    public void send(String subject, String html, List<String> to) throws Exception {
        if (mailgun.isConfigured()) {
            mailgun.send(sender.orElse("").trim(), to, subject, html);
            return;
        }
        mailer.send(new Mail().setSubject(subject).setHtml(html).setTo(to));
    }
}
