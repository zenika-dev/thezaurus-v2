package com.zenika.thezaurus.service;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.ReminderTemplatePreview;
import com.zenika.thezaurus.model.ReminderTemplateView;
import com.zenika.thezaurus.model.Talk;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/** Shared, side-effect-free renderer for previews and future delivery. No Java objects reach Qute. */
@ApplicationScoped
public class ReminderTemplateRenderer {
    private static final Set<String> VARIABLES = Set.of("talkTitle", "talkDate", "conferenceName", "talksUrl");
    private static final Set<String> CONDITIONS = Set.of("hasConference", "hasDate", "missingVideo", "missingAudience");
    private static final Pattern SIZE =
            Pattern.compile("font-size\\s*:\\s*(12|14|16|18|24|32)px\\s*;?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTROL =
            Pattern.compile("\\{(?:#if (?:hasConference|hasDate|missingVideo|missingAudience)|#else|/if)}");
    private static final String URL_MARKER = "https://thezaurus-template.invalid/talks";
    private final Engine engine = Engine.builder().addDefaults().build();

    @ConfigProperty(name = "thezaurus.public-url", defaultValue = "http://localhost:3000")
    String publicUrl;

    public ReminderTemplateView validate(String subject, String bodyHtml) {
        if (subject == null
                || subject.isBlank()
                || subject.length() > 500
                || subject.contains("\n")
                || subject.contains("\r")) {
            throw invalid("Le sujet est requis, sur une seule ligne (500 caractères maximum).");
        }
        if (bodyHtml == null || bodyHtml.isBlank() || bodyHtml.length() > 100_000) {
            throw invalid("Le corps est requis (100 000 caractères maximum).");
        }
        validateSyntax(subject);
        validateSyntax(bodyHtml);
        String clean = sanitize(bodyHtml, true);
        if (CONTROL.matcher(Jsoup.parseBodyFragment(clean).text())
                .replaceAll("")
                .replace('\u00a0', ' ')
                .isBlank()) {
            throw invalid("Le corps du message ne peut pas être vide.");
        }
        // Sanitization can change HTML boundaries: validate the actual persisted representation too.
        validateSyntax(clean);
        return new ReminderTemplateView(subject, clean, 0);
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

    public ReminderTemplatePreview render(String subject, String bodyHtml, Talk talk) {
        ReminderTemplateView template = validate(subject, bodyHtml);
        String date = date(talk.date());
        String conference =
                talk.conference() == null ? "" : text(talk.conference().getName());
        Map<String, Object> data = new HashMap<>();
        data.put("talkTitle", text(talk.title()));
        data.put("talkDate", date);
        data.put("conferenceName", conference);
        data.put("talksUrl", talksUrl());
        data.put("hasDate", !date.isBlank());
        data.put("hasConference", !conference.isBlank());
        data.put("missingVideo", text(talk.replay()).isBlank());
        data.put("missingAudience", talk.audience() == null);
        String renderedSubject = engine.parse(template.subject())
                .data(data)
                .render()
                .replace('\r', ' ')
                .replace('\n', ' ');
        Map<String, Object> htmlData = new HashMap<>(data);
        VARIABLES.forEach(key -> htmlData.put(key, escape((String) data.get(key))));
        Document document = Jsoup.parseBodyFragment(template.bodyHtml());
        document.outputSettings().prettyPrint(false);
        // Tiptap places block controls in separate paragraphs; strip those wrappers before rendering.
        document.select("p").forEach(p -> {
            if (CONTROL.matcher(p.text().trim()).matches()) {
                p.before(new org.jsoup.nodes.TextNode(p.text().trim()));
                p.remove();
            }
        });
        String html = engine.parse(document.body().html()).data(htmlData).render();
        List<String> to = talk.speakers() == null
                ? List.of()
                : talk.speakers().stream()
                        .filter(Objects::nonNull)
                        .map(user -> text(user.email()).trim())
                        .filter(email -> !email.isEmpty())
                        .distinct()
                        .toList();
        return new ReminderTemplatePreview(renderedSubject, sanitize(html, false), to);
    }

    private String talksUrl() {
        URI uri = URI.create(publicUrl);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalStateException("thezaurus.public-url doit être une URL HTTP(S) absolue.");
        }
        return publicUrl.replaceAll("/+$", "") + "/talks";
    }

    private static String sanitize(String html, boolean template) {
        Document document = Jsoup.parseBodyFragment(html);
        document.select("[style]").forEach(element -> {
            var matcher = SIZE.matcher(element.attr("style").trim());
            if (element.normalName().equals("span") && matcher.matches())
                element.attr("style", "font-size: " + matcher.group(1) + "px");
            else element.removeAttr("style");
        });
        if (template) {
            document.getAllElements()
                    .forEach(element -> element.attributes().forEach(attribute -> {
                        if (attribute.getValue().contains("{")
                                || attribute.getValue().contains("}")) {
                            if (element.normalName().equals("a")
                                    && attribute.getKey().equals("href")
                                    && attribute.getValue().equals("{talksUrl}")) {
                                element.attr("href", URL_MARKER);
                            } else
                                throw invalid(
                                        "Seule la variable {talksUrl} est autorisée dans un lien ; les autres variables doivent être dans le texte.");
                        }
                    }));
        }
        Safelist allowed = new Safelist()
                .addTags("p", "br", "strong", "b", "em", "i", "u", "ul", "ol", "li", "a", "span")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https")
                .addAttributes("span", "style");
        Document clean = new Cleaner(allowed).clean(document);
        clean.outputSettings().prettyPrint(false);
        if (template)
            clean.select("a[href]").forEach(a -> {
                if (a.attr("href").equals(URL_MARKER)) a.attr("href", "{talksUrl}");
            });
        return clean.body().html();
    }

    private static String escape(String value) {
        return Entities.escape(value).replace("\"", "&quot;").replace("'", "&#39;");
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
