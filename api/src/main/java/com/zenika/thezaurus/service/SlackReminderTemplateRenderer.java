package com.zenika.thezaurus.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.ReminderTemplatePreview;
import com.zenika.thezaurus.model.ReminderTemplateView;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SlackReminderTemplateRenderer {
    private static final Pattern CONTROL = Pattern.compile("\\{\\{\\s*(?:[#^/][a-zA-Z0-9_]+)\\s*}}");

    private final MustacheFactory plainMustacheFactory = new DefaultMustacheFactory() {
        @Override
        public void encode(String value, Writer writer) {
            try {
                writer.write(value);
            } catch (IOException e) {
                throw new MustacheException(e);
            }
        }
    };

    @ConfigProperty(name = "thezaurus.public-url", defaultValue = "http://localhost:3000")
    String publicUrl;

    public ReminderTemplateView validate(String body) {
        if (body == null || body.isBlank() || body.length() > 100_000) {
            throw invalid("Le corps du message est requis (100 000 caractères maximum).");
        }
        compile(body);
        String stripped = CONTROL.matcher(body).replaceAll("").trim();
        if (stripped.isBlank()) {
            throw invalid("Le corps du message ne peut pas être vide.");
        }
        return new ReminderTemplateView("", body.trim(), 0);
    }

    private Mustache compile(String source) {
        try {
            return plainMustacheFactory.compile(new StringReader(source), "slack-template");
        } catch (MustacheException exception) {
            throw invalid("Syntaxe Mustache invalide : " + exception.getMessage());
        }
    }

    public String render(String body, Talk talk, User recipient) {
        validate(body);
        String date = date(talk.date());
        String conference =
                talk.conference() == null ? "" : text(talk.conference().getName());

        Map<String, Object> data = new HashMap<>();
        String mention = mention(recipient);
        data.put("recipientMention", mention);
        data.put("speakerName", recipient == null ? "" : text(recipient.name()));
        data.put("talkTitle", text(talk.title()));
        data.put("talkDate", date);
        data.put("conferenceName", conference);
        data.put("talkUrl", talkUrl(talk.id()));
        data.put("talksUrl", talksUrl());
        data.put("hasDate", !date.isBlank());
        data.put("hasConference", !conference.isBlank());
        data.put("missingReplay", text(talk.replay()).isBlank());
        data.put("missingAudience", talk.audience() == null);
        data.put(
                "speakers",
                talk.speakers() == null
                        ? ""
                        : talk.speakers().stream()
                                .filter(Objects::nonNull)
                                .map(speaker -> text(speaker.name()).trim())
                                .filter(name -> !name.isEmpty())
                                .distinct()
                                .collect(Collectors.joining(", ")));

        StringWriter writer = new StringWriter();
        compile(body).execute(writer, data);
        return writer.toString().trim();
    }

    public ReminderTemplatePreview renderPreview(String body, Talk talk) {
        User speaker = (talk.speakers() != null && !talk.speakers().isEmpty())
                ? talk.speakers().getFirst()
                : User.builder().name("Speaker").build();
        User previewRecipient =
                (speaker.slackUserId() != null && !speaker.slackUserId().isBlank())
                        ? speaker
                        : speaker.withSlackUserId(
                                speaker.name() != null && !speaker.name().isBlank()
                                        ? speaker.name().trim()
                                        : "speaker");
        String rendered = render(body, talk, previewRecipient);
        List<String> to = talk.speakers() == null
                ? List.of()
                : talk.speakers().stream()
                        .filter(Objects::nonNull)
                        .map(user -> {
                            String name = text(user.name()).trim();
                            String email = text(user.email()).trim();
                            if (!name.isEmpty() && !email.isEmpty()) return name + " (" + email + ")";
                            if (!email.isEmpty()) return email;
                            return name;
                        })
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();
        return new ReminderTemplatePreview("", rendered, to);
    }

    private static String mention(User recipient) {
        if (recipient == null) return "@Speaker";
        if (recipient.slackUserId() != null && !recipient.slackUserId().isBlank()) {
            return "<@" + recipient.slackUserId().trim() + ">";
        }
        if (recipient.name() != null && !recipient.name().isBlank()) {
            return "@" + recipient.name().trim();
        }
        return "@Speaker";
    }

    private String talksUrl() {
        URI uri = URI.create(publicUrl);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalStateException("thezaurus.public-url doit être une URL HTTP(S) absolue.");
        }
        return publicUrl.replaceAll("/+$", "") + "/talks";
    }

    private String talkUrl(String talkId) {
        if (talkId == null || talkId.isBlank()) {
            return talksUrl();
        }
        return talksUrl() + "/" + talkId.trim();
    }

    private static String date(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.FRENCH));
        } catch (DateTimeParseException ignored) {
            return "";
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    public static ThezaurusException invalid(String message) {
        return new ThezaurusException(message, Response.Status.BAD_REQUEST);
    }
}
