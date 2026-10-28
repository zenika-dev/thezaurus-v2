package com.zenika.thezaurus.visuals.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.ConferenceInput;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.VisualRequest;
import com.zenika.thezaurus.visuals.model.VisualTemplate;
import com.zenika.thezaurus.visuals.model.VisualType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateServiceTest {

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(new ObjectMapper(), Optional.empty());
    }

    @Test
    void shouldLoadAllTemplatesAndFilterByCategory() {
        List<VisualTemplate> all = templateService.listTemplates(null);
        assertEquals(5, all.size());

        List<VisualTemplate> talkTemplates = templateService.listTemplates(VisualType.TALK);
        assertEquals(3, talkTemplates.size());
        assertTrue(talkTemplates.stream().allMatch(t -> t.category() == VisualType.TALK));

        List<VisualTemplate> blogTemplates = templateService.listTemplates(VisualType.BLOG);
        assertEquals(2, blogTemplates.size());
        assertTrue(blogTemplates.stream().allMatch(t -> t.category() == VisualType.BLOG));
    }

    @Test
    void shouldRouteTalkTemplatesBasedOnSpeakersCount() {
        VisualRequest single =
                VisualRequest.forTalk("Quarkus in Action", "Devoxx France", List.of(new PersonInput("Jean Dupont")));
        assertEquals(
                TemplateService.DEFAULT_TALK_TEMPLATE,
                templateService.resolveTemplate(single).name());

        VisualRequest duo = VisualRequest.forTalk(
                "Quarkus in Action",
                "Devoxx France",
                List.of(new PersonInput("Jean Dupont"), new PersonInput("Marie Curie")));
        assertEquals(
                TemplateService.DEFAULT_TALK_DUO_TEMPLATE,
                templateService.resolveTemplate(duo).name());

        VisualRequest trio = VisualRequest.forTalk(
                "Quarkus in Action",
                "Devoxx France",
                List.of(
                        new PersonInput("Jean Dupont"),
                        new PersonInput("Marie Curie"),
                        new PersonInput("Alan Turing")));
        assertEquals(
                TemplateService.DEFAULT_TALK_TRIO_TEMPLATE,
                templateService.resolveTemplate(trio).name());
    }

    @Test
    void shouldRouteBlogTemplatesBasedOnWritersCount() {
        VisualRequest single = VisualRequest.forBlog("Article Tech", List.of(new PersonInput("Jean Dupont")));
        assertEquals(
                TemplateService.DEFAULT_BLOG_TEMPLATE,
                templateService.resolveTemplate(single).name());

        VisualRequest duo = VisualRequest.forBlog(
                "Article Tech", List.of(new PersonInput("Jean Dupont"), new PersonInput("Marie Curie")));
        assertEquals(
                TemplateService.DEFAULT_BLOG_DUO_TEMPLATE,
                templateService.resolveTemplate(duo).name());
    }

    @Test
    void shouldAutoDetectTypeWhenOmitted() {
        // Détection Talk via présence de conference
        VisualRequest talkReq = new VisualRequest(
                "Mon Talk", null, new ConferenceInput("Devoxx"), List.of(new PersonInput("Jean")), null);
        assertEquals(VisualType.TALK, templateService.determineCategory(talkReq));
        assertEquals(
                TemplateService.DEFAULT_TALK_TEMPLATE,
                templateService.resolveTemplate(talkReq).name());

        // Détection Blog via présence de writers uniquement
        VisualRequest blogReq = new VisualRequest("Mon Blog", null, null, null, List.of(new PersonInput("Jean")));
        assertEquals(VisualType.BLOG, templateService.determineCategory(blogReq));
        assertEquals(
                TemplateService.DEFAULT_BLOG_TEMPLATE,
                templateService.resolveTemplate(blogReq).name());
    }

    @Test
    void shouldRejectInvalidSpeakerOrWriterCounts() {
        VisualRequest noSpeakers = VisualRequest.forTalk("Talk", "Devoxx", List.of());
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTemplate(noSpeakers));

        VisualRequest tooManySpeakers = VisualRequest.forTalk(
                "Talk",
                "Devoxx",
                List.of(new PersonInput("A"), new PersonInput("B"), new PersonInput("C"), new PersonInput("D")));
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTemplate(tooManySpeakers));

        VisualRequest noWriters = VisualRequest.forBlog("Blog", List.of());
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTemplate(noWriters));

        VisualRequest tooManyWriters = VisualRequest.forBlog(
                "Blog", List.of(new PersonInput("A"), new PersonInput("B"), new PersonInput("C")));
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTemplate(tooManyWriters));
    }

    @Test
    void shouldRejectTalkWithoutConference() {
        VisualRequest missingConf = new VisualRequest("Talk", "talk", null, List.of(new PersonInput("Jean")), null);
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTemplate(missingConf));
    }

    @Test
    void shouldBuildTalkAndBlogPromptsWithAllPlaceholdersReplaced() {
        VisualRequest talkRequest = VisualRequest.forTalk(
                "Les agents IA avec Quarkus", "Sunny Tech 2026", List.of(new PersonInput("Jean-Phi", "photo-base64")));
        VisualTemplate talkTemplate = templateService.resolveTemplate(talkRequest);
        String talkPrompt = templateService.buildPrompt(talkTemplate, talkRequest);

        assertTrue(talkPrompt.contains("Les agents IA avec Quarkus"));
        assertTrue(talkPrompt.contains("Jean-Phi"));
        assertTrue(talkPrompt.contains("Sunny Tech 2026"));
        assertFalse(talkPrompt.contains("%TITLE%"));
        assertFalse(talkPrompt.contains("%NAME%"));
        assertFalse(talkPrompt.contains("%CONFERENCE%"));

        VisualRequest blogRequest =
                VisualRequest.forBlog("Découvrir Google GenAI en Java", List.of(new PersonInput("Jean-Phi")));
        VisualTemplate blogTemplate = templateService.resolveTemplate(blogRequest);
        String blogPrompt = templateService.buildPrompt(blogTemplate, blogRequest);

        assertTrue(blogPrompt.contains("Découvrir Google GenAI en Java"));
        assertTrue(blogPrompt.contains("Jean-Phi"));
        assertFalse(blogPrompt.contains("%TITLE%"));
        assertFalse(blogPrompt.contains("%NAME%"));
    }

    @Test
    void shouldLoadTemplateImageFromClasspathOrExternalDirectory(@TempDir Path tempDir) throws IOException {
        VisualTemplate talkTemplate = templateService
                .findByName(TemplateService.DEFAULT_TALK_TEMPLATE)
                .orElseThrow();
        InlineMediaPart classpathMedia = templateService.loadTemplateImage(talkTemplate);
        assertNotNull(classpathMedia);
        assertEquals("image/png", classpathMedia.mimeType());
        assertTrue(classpathMedia.data().length > 0);

        Path customTemplatesFolder = tempDir.resolve("templates");
        Files.createDirectories(customTemplatesFolder);
        byte[] customBytes = new byte[] {1, 2, 3, 4, 5};
        Files.write(customTemplatesFolder.resolve("talk-single.png"), customBytes);

        TemplateService customService = new TemplateService(new ObjectMapper(), Optional.of(tempDir.toString()));
        InlineMediaPart externalMedia = customService.loadTemplateImage(talkTemplate);
        assertEquals(5, externalMedia.data().length);
    }
}
