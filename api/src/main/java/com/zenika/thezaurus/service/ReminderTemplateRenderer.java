package com.zenika.thezaurus.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.ReminderTemplatePreview;
import com.zenika.thezaurus.model.ReminderTemplateView;
import com.zenika.thezaurus.model.Talk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

/** Shared, side-effect-free renderer for previews and future delivery using Mustache. */
@ApplicationScoped
public class ReminderTemplateRenderer {
    private static final Set<String> VARIABLES =
            Set.of("talkTitle", "talkDate", "conferenceName", "talksUrl", "speakers");
    private static final Pattern SIZE =
            Pattern.compile("font-size\\s*:\\s*(12|14|16|18|24|32)px\\s*;?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTROL = Pattern.compile("\\{\\{\\s*(?:[#^/][a-zA-Z0-9_]+)\\s*}}");
    private static final String URL_MARKER = "https://thezaurus-template.invalid/talks";
    private final MustacheFactory htmlMustacheFactory = new DefaultMustacheFactory();
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
        compileSubject(subject);
        compileHtml(bodyHtml);
        String clean = sanitize(bodyHtml, true);
        if (CONTROL.matcher(Jsoup.parseBodyFragment(clean).text())
                .replaceAll("")
                .replace('\u00a0', ' ')
                .isBlank()) {
            throw invalid("Le corps du message ne peut pas être vide.");
        }
        compileHtml(clean);
        return new ReminderTemplateView(subject, clean, 0);
    }

    private Mustache compileSubject(String source) {
        try {
            return plainMustacheFactory.compile(new StringReader(source), "subject");
        } catch (MustacheException exception) {
            throw invalid("Syntaxe Mustache invalide : " + exception.getMessage());
        }
    }

    private Mustache compileHtml(String source) {
        try {
            return htmlMustacheFactory.compile(new StringReader(source), "template");
        } catch (MustacheException exception) {
            throw invalid("Syntaxe Mustache invalide : " + exception.getMessage());
        }
    }

    public ReminderTemplatePreview render(String subject, String bodyHtml, Talk talk) {
        ReminderTemplateView template = validate(subject, bodyHtml);
        String date = date(effectiveDate(talk));
        String conference =
                talk.conference() == null ? "" : text(talk.conference().getName());
        Map<String, Object> data = new HashMap<>();
        data.put("talkTitle", text(talk.title()));
        data.put("talkDate", date);
        data.put("conferenceName", conference);
        data.put("talksUrl", talksUrl());
        data.put("hasDate", !date.isBlank());
        data.put("hasConference", !conference.isBlank());
        data.put("missingReplay", text(talk.replay()).isBlank());
        data.put("missingVideo", data.get("missingReplay"));
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

        String renderedSubject = renderTemplate(compileSubject(template.subject()), data)
                .replace('\r', ' ')
                .replace('\n', ' ');

        Document document = Jsoup.parseBodyFragment(template.bodyHtml());
        document.outputSettings().prettyPrint(false);
        // Tiptap places block controls in separate paragraphs; strip those wrappers before rendering.
        document.select("p").forEach(p -> {
            if (CONTROL.matcher(p.text().trim()).matches()) {
                p.before(new org.jsoup.nodes.TextNode(p.text().trim()));
                p.remove();
            }
        });
        String html = renderTemplate(compileHtml(document.body().html()), data);
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

    private static String renderTemplate(Mustache mustache, Map<String, Object> data) {
        StringWriter writer = new StringWriter();
        mustache.execute(writer, data);
        return writer.toString();
    }

    private String talksUrl() {
        URI uri = URI.create(publicUrl);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalStateException("thezaurus.public-url doit être une URL HTTP(S) absolue.");
        }
        String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
        return uri.resolve(path + "/talks").toString();
    }

    private static String sanitize(String html, boolean template) {
        Document document = Jsoup.parseBodyFragment(html);
        document.select("[style]").forEach(element -> {
            var matcher = SIZE.matcher(element.attr("style").trim());
            if (element.normalName().equals("span") && matcher.matches()) {
                element.attr("style", "font-size: " + matcher.group(1) + "px");
            } else {
                element.removeAttr("style");
            }
        });
        if (template) {
            document.getAllElements()
                    .forEach(element -> element.attributes().forEach(attribute -> {
                        if (attribute.getValue().contains("{")
                                || attribute.getValue().contains("}")) {
                            if (element.normalName().equals("a")
                                    && attribute.getKey().equals("href")
                                    && attribute.getValue().equals("{{talksUrl}}")) {
                                element.attr("href", URL_MARKER);
                            } else {
                                throw invalid(
                                        "Seule la variable {{talksUrl}} est autorisée dans un lien ; les autres variables doivent être dans le texte.");
                            }
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
        if (template) {
            clean.select("a[href]").forEach(a -> {
                if (a.attr("href").equals(URL_MARKER)) {
                    a.attr("href", "{{talksUrl}}");
                }
            });
        }
        return clean.body().html();
    }

    private static String effectiveDate(Talk talk) {
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

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String date(String date) {
        if (date == null || date.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(date.trim()).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
        } catch (Exception exception) {
            return date.trim();
        }
    }

    public static ThezaurusException invalid(String message) {
        return new ThezaurusException(message, Response.Status.BAD_REQUEST);
    }
}
