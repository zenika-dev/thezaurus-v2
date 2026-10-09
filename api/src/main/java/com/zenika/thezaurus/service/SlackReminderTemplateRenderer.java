package com.zenika.thezaurus.service;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.ReminderTemplatePreview;
import com.zenika.thezaurus.model.ReminderTemplateView;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.User;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SlackReminderTemplateRenderer {
    private static final Set<String> VARIABLES = Set.of(
            "recipientMention",
            "speakerName",
            "speakers",
            "talkTitle",
            "talkDate",
            "conferenceName",
            "talkUrl",
            "talksUrl");

    private static final Set<String> CONDITIONS = ReminderTemplateMetadata.CONDITIONS.stream()
            .map(condition -> condition.name())
            .collect(Collectors.toUnmodifiableSet());

    private static final Pattern CONTROL = Pattern.compile("\\{\\s*(?:#if\\s+(?:"
            + CONDITIONS.stream().map(Pattern::quote).collect(Collectors.joining("|"))
            + ")|#else|/if)\\s*}");

    private final Engine engine = Engine.builder().addDefaults().build();

    @ConfigProperty(name = "thezaurus.public-url", defaultValue = "http://localhost:3000")
    String publicUrl;

    public ReminderTemplateView validate(String body) {
        if (body == null || body.isBlank() || body.length() > 100_000) {
            throw invalid("Le corps du message est requis (100 000 caractères maximum).");
        }
        validateSyntax(body);
        String stripped = CONTROL.matcher(body).replaceAll("").trim();
        if (stripped.isBlank()) {
            throw invalid("Le corps du message ne peut pas être vide.");
        }
        return new ReminderTemplateView("", body.trim(), 0);
    }

    private void validateSyntax(String source) {
        int depth = 0;
        boolean[] hasElse = new boolean[20];
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '}') throw invalid("Accolade fermante Qute inattendue.");
            if (source.charAt(i) != '{') continue;
            int end = source.indexOf('}', i);
            if (end < 0) throw invalid("Balise Qute non fermée.");
            String token = source.substring(i + 1, end).trim();
            if (token.startsWith("#if ")) {
                if (!CONDITIONS.contains(token.substring(4).trim()))
                    throw invalid("Condition Qute inconnue : " + token);
                if (++depth > 20) throw invalid("Trop de conditions imbriquées (maximum 20).");
                hasElse[depth - 1] = false;
            } else if (token.equals("/if")) {
                if (--depth < 0) throw invalid("Fermeture de condition Qute inattendue.");
            } else if (token.equals("#else")) {
                if (depth == 0 || hasElse[depth - 1]) throw invalid("Bloc else Qute inattendu ou multiple.");
                hasElse[depth - 1] = true;
            } else if (!VARIABLES.contains(token)) {
                throw invalid("Variable ou syntaxe Qute non autorisée : " + token);
            }
            i = end;
        }
        if (depth != 0) throw invalid("Condition Qute non fermée.");
        try {
            engine.parse(source);
        } catch (TemplateException exception) {
            throw invalid("Syntaxe Qute invalide : " + exception.getMessage());
        }
    }

    public String render(String body, Talk talk, User recipient) {
        validate(body);
        String date = date(talk.date());
        String conference =
                talk.conference() == null ? "" : text(talk.conference().getName());
        String url = talksUrl();

        Map<String, Object> data = new HashMap<>();
        String mention = mention(recipient);
        data.put("recipientMention", mention);
        data.put("speakerName", recipient == null ? "" : text(recipient.name()));
        data.put("talkTitle", text(talk.title()));
        data.put("talkDate", date);
        data.put("conferenceName", conference);
        data.put("talkUrl", url);
        data.put("talksUrl", url);
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

        return engine.parse(body).data(data).render().trim();
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
