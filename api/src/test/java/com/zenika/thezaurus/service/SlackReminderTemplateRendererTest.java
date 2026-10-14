package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SlackReminderTemplateRendererTest {
    private final SlackReminderTemplateRenderer renderer = new SlackReminderTemplateRenderer();

    SlackReminderTemplateRendererTest() {
        renderer.publicUrl = "https://thezaurus.example/";
    }

    private Talk talk(String title, String date, Conference conference, String replay, Integer audience) {
        return new Talk(
                "one",
                title,
                "description",
                List.of(
                        User.builder()
                                .name("Alice")
                                .email("alice@zenika.com")
                                .slackUserId("U123")
                                .build(),
                        User.builder()
                                .name("Bob")
                                .email("bob@zenika.com")
                                .slackUserId("U456")
                                .build()),
                "Lyon",
                conference,
                TalkStatus.DONE,
                Visibility.PUBLIC,
                "format",
                date,
                null,
                null,
                null,
                replay,
                audience);
    }

    @Test
    void rendersRecipientMentionAndTalkVariables() {
        User recipient = User.builder()
                .name("Alice")
                .email("alice@zenika.com")
                .slackUserId("U123")
                .build();
        var talk = talk("Introduction à Quarkus", "2026-09-16", new Conference("c", "DevFest", null), null, null);

        String template = "Bonjour {{recipientMention}} (ou {{speakerName}}) !\n"
                + "{{#hasConference}}Bravo pour {{conferenceName}} le {{talkDate}} !{{/hasConference}}\n"
                + "Co-speakers : {{speakers}}\n"
                + "Renseigne le replay sur <{{talkUrl}}|Thezaurus>.";

        String rendered = renderer.render(template, talk, recipient);

        assertTrue(rendered.contains("Bonjour <@U123> (ou Alice) !"));
        assertTrue(rendered.contains("Bravo pour DevFest le 16 septembre 2026 !"));
        assertTrue(rendered.contains("Co-speakers : Alice, Bob"));
        assertTrue(rendered.contains("https://thezaurus.example/talks"));
    }

    @Test
    void fallbackRecipientMentionWhenSlackUserIdIsMissing() {
        User recipient = User.builder().name("Alice").email("alice@zenika.com").build();
        var talk = talk("Talk", null, null, null, null);

        String rendered = renderer.render("Hello {{recipientMention}}", talk, recipient);

        assertEquals("Hello @Alice", rendered);
    }

    @Test
    void conditionsEvaluateMissingValuesCorrectly() {
        User recipient = User.builder().name("Alice").slackUserId("U123").build();
        String template = "{{#missingReplay}}Replay manquant{{/missingReplay}}"
                + "{{#missingAudience}}Audience manquante{{/missingAudience}}"
                + "{{#hasConference}}Avec conf{{/hasConference}}{{^hasConference}}Sans conf{{/hasConference}}";

        var completeTalk = talk("Talk", "2026-09-16", null, "https://replay.example", 0);
        String completeRendered = renderer.render(template, completeTalk, recipient);
        assertEquals("Sans conf", completeRendered);

        var missingTalk = talk("Talk", null, null, null, null);
        String missingRendered = renderer.render(template, missingTalk, recipient);
        assertTrue(missingRendered.contains("Replay manquant"));
        assertTrue(missingRendered.contains("Audience manquante"));
        assertTrue(missingRendered.contains("Sans conf"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{{/hasDate}}orphan", "{{#hasDate}}{{/other}}"})
    void rejectsInvalidOrUnsafeSyntax(String body) {
        assertThrows(ThezaurusException.class, () -> renderer.validate(body));
    }

    @Test
    void rejectsEmptyOrBlankBody() {
        assertThrows(ThezaurusException.class, () -> renderer.validate(""));
        assertThrows(ThezaurusException.class, () -> renderer.validate("   "));
        assertThrows(ThezaurusException.class, () -> renderer.validate("{{#hasDate}}{{/hasDate}}"));
    }

    @Test
    void previewRendersFirstSpeakerAndListsAllSpeakers() {
        var talk = talk("Super Talk", "2026-09-16", new Conference("c", "DevFest", null), null, null);
        String template = "Salut {{recipientMention}}, bravo pour *{{talkTitle}}* !";

        var preview = renderer.renderPreview(template, talk);

        assertEquals("", preview.subject());
        assertTrue(preview.bodyHtml().contains("Salut <@U123>, bravo pour *Super Talk* !"));
        assertEquals(List.of("Alice (alice@zenika.com)", "Bob (bob@zenika.com)"), preview.to());
    }

    @Test
    void rendersNativeMarkdownTemplate() {
        User recipient = User.builder()
                .name("Alice")
                .email("alice@zenika.com")
                .slackUserId("U123")
                .build();
        var talk = talk("Quarkus 3", "2026-09-16", new Conference("c", "DevFest", null), null, null);

        String markdownTemplate = "*Hello {{recipientMention}}*\n"
                + "Bravo pour *{{talkTitle}}* à _{{conferenceName}}_ !\n"
                + "Consulte la liste sur <{{talksUrl}}|Thezaurus>.\n"
                + "• Point 1\n• Point 2";

        String rendered = renderer.render(markdownTemplate, talk, recipient);

        assertEquals(
                "*Hello <@U123>*\n"
                        + "Bravo pour *Quarkus 3* à _DevFest_ !\n"
                        + "Consulte la liste sur <https://thezaurus.example/talks|Thezaurus>.\n"
                        + "• Point 1\n• Point 2",
                rendered);
    }
}
