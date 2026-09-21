package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.*;

import com.zenika.thezaurus.exception.ThezaurusException;
import com.zenika.thezaurus.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReminderTemplateRendererTest {
    private final ReminderTemplateRenderer renderer = new ReminderTemplateRenderer();

    ReminderTemplateRendererTest() {
        renderer.publicUrl = "https://thezaurus.example/";
    }

    private Talk talk(String title, String date, Conference conference, String replay, Integer audience) {
        return new Talk(
                "one",
                title,
                "description",
                List.of(
                        User.builder().email("one@zenika.com").build(),
                        User.builder().email("two@zenika.com").build(),
                        User.builder().email("").build()),
                "office",
                conference,
                TalkStatus.DRAFT,
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
    void rendersAllSpeakersEscapesDataAndFormatsFrenchDate() {
        var result = renderer.render(
                "Rappel {talkTitle}",
                "<p>{talkTitle} — {talkDate} — {conferenceName}</p><a href=\"{talksUrl}\">Ouvrir</a>",
                talk(
                        "<script>alert('x')</script>",
                        "2026-09-16",
                        new Conference("c", "Java & friends", null),
                        null,
                        null));
        assertEquals(List.of("one@zenika.com", "two@zenika.com"), result.to());
        assertTrue(result.bodyHtml().contains("&lt;script&gt;"));
        assertFalse(result.bodyHtml().contains("<script>"));
        assertTrue(result.bodyHtml().contains("16 septembre 2026"));
        assertTrue(result.bodyHtml().contains("Java &amp; friends"));
        assertTrue(result.bodyHtml().contains("https://thezaurus.example/talks"));
    }

    @Test
    void conditionsUseActualMissingValuesIncludingZeroAudience() {
        String body =
                "<p><strong>{#if hasDate}</strong></p><p>Date {talkDate}</p><p>{#else}</p><p>Sans date</p><p>{/if}</p>"
                        + "{#if hasConference}Conférence{/if}{#if missingVideo}Vidéo{/if}{#if missingAudience}Audience{/if}";
        var result = renderer.render("Sujet", body, talk("Talk", null, null, "https://video.example", 0));
        assertEquals("<p>Sans date</p>", result.bodyHtml());
        var missing = renderer.render(
                "Sujet", body, talk("Talk", "2026-09-16", new Conference("c", "Conf", null), null, null));
        assertTrue(missing.bodyHtml().contains("ConférenceVidéoAudience"));
        assertFalse(missing.bodyHtml().contains("<p></p>"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{#if hasDate}{unknown}{/if}",
                "{talkTitle.toString()}",
                "{inject:bean}",
                "{#for x in talkTitle}{x}{/for}",
                "{#if hasDate}missing",
                "{#else}invalid",
                "{#if hasDate}a{#else}b{#else}c{/if}",
                "{talkTitle.raw}",
                "{#if talkTitle}bad{/if}",
                "{#if hasDate && missingVideo}bad{/if}"
            })
    void rejectsInvalidOrUnsafeSyntaxInEveryBranch(String body) {
        assertThrows(ThezaurusException.class, () -> renderer.validate("Sujet", body));
    }

    @Test
    void sanitizesActiveHtmlAndOnlyRetainsAllowedFontSizes() {
        var clean = renderer.validate(
                "Sujet",
                "<script>alert(1)</script><p onclick=\"bad()\"><strong>Texte</strong><img src=x onerror=alert(1)><a href=\"javascript:alert(1)\">Lien</a><span style=\"font-size: 24px\">grand</span><span style=\"font-size: 99px; color:red\">autre</span></p>");
        assertEquals(
                "<p><strong>Texte</strong><a>Lien</a><span style=\"font-size: 24px\">grand</span><span>autre</span></p>",
                clean.bodyHtml());
    }

    @Test
    void rejectsEmptyContentAndUnsafeAttributes() {
        assertThrows(ThezaurusException.class, () -> renderer.validate("", "text"));
        assertThrows(ThezaurusException.class, () -> renderer.validate("Subject", "<p><br></p>"));
        assertThrows(ThezaurusException.class, () -> renderer.validate("Subject", "<p>&nbsp;</p>"));
        assertThrows(ThezaurusException.class, () -> renderer.validate("Subject", "<a href=\"{talkTitle}\">link</a>"));
        assertThrows(ThezaurusException.class, () -> renderer.validate("Subject", "{#if hasDate}{/if}"));
    }

    @Test
    void absentOptionalDataRendersEmptyText() {
        assertEquals(
                "<p>Titre /  / </p>",
                renderer.render(
                                "Sujet",
                                "<p>{talkTitle} / {talkDate} / {conferenceName}</p>",
                                talk("Titre", null, null, null, null))
                        .bodyHtml());
    }

    @Test
    void spacedConditionControlsDoNotCountAsContentOrLeaveParagraphs() {
        assertThrows(ThezaurusException.class, () -> renderer.validate("Sujet", "<p>{#if hasDate }</p><p>{/if}</p>"));
        var result = renderer.render(
                "Sujet",
                "<p>{#if hasDate }</p><p>Avec date</p><p>{#else}</p><p>Sans date</p><p>{/if}</p>",
                talk("Talk", null, null, null, null));
        assertEquals("<p>Sans date</p>", result.bodyHtml());
    }
}
