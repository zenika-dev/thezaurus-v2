package com.zenika.thezaurus.visuals.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.ConferenceInput;
import com.zenika.thezaurus.visuals.model.NightClazzVisualRequest;
import com.zenika.thezaurus.visuals.model.PartnerInput;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.TalkVisualRequest;
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
        assertEquals(8, all.size());

        List<VisualTemplate> talkTemplates = templateService.listTemplates(VisualType.TALK);
        assertEquals(4, talkTemplates.size());
        assertTrue(talkTemplates.stream().allMatch(t -> t.category() == VisualType.TALK));

        List<VisualTemplate> blogTemplates = templateService.listTemplates(VisualType.BLOG);
        assertEquals(2, blogTemplates.size());
        assertTrue(blogTemplates.stream().allMatch(t -> t.category() == VisualType.BLOG));

        List<VisualTemplate> nightClazzTemplates = templateService.listTemplates(VisualType.NIGHT_CLAZZ);
        assertEquals(2, nightClazzTemplates.size());
        assertTrue(nightClazzTemplates.stream().allMatch(t -> t.category() == VisualType.NIGHT_CLAZZ));
    }

    @Test
    void shouldLoadAllClasspathTemplateImagesSuccessfully() {
        for (VisualTemplate template : templateService.listTemplates(null)) {
            InlineMediaPart image = templateService.loadTemplateImage(template);
            assertNotNull(image, "Image introuvable pour le template " + template.name());
            assertEquals("image/png", image.mimeType());
            assertTrue(image.data().length > 0, "Image vide pour le template " + template.name());
        }
    }

    @Test
    void shouldRouteTalkTemplatesBasedOnSpeakersCount() {
        TalkVisualRequest single =
                new TalkVisualRequest("Quarkus in Action", "Devoxx France", List.of(new PersonInput("Jean Dupont")));
        assertEquals(
                TemplateService.DEFAULT_TALK_TEMPLATE,
                templateService.resolveTalkTemplate(single).name());

        TalkVisualRequest duo = new TalkVisualRequest(
                "Quarkus in Action",
                "Devoxx France",
                List.of(new PersonInput("Jean Dupont"), new PersonInput("Marie Curie")));
        assertEquals(
                TemplateService.DEFAULT_TALK_DUO_TEMPLATE,
                templateService.resolveTalkTemplate(duo).name());

        TalkVisualRequest trio = new TalkVisualRequest(
                "Quarkus in Action",
                "Devoxx France",
                List.of(
                        new PersonInput("Jean Dupont"),
                        new PersonInput("Marie Curie"),
                        new PersonInput("Alan Turing")));
        assertEquals(
                TemplateService.DEFAULT_TALK_TRIO_TEMPLATE,
                templateService.resolveTalkTemplate(trio).name());
    }

    @Test
    void shouldRouteBlogTemplatesBasedOnWritersCount() {
        BlogVisualRequest single = new BlogVisualRequest("Article Tech", List.of(new PersonInput("Jean Dupont")));
        assertEquals(
                TemplateService.DEFAULT_BLOG_TEMPLATE,
                templateService.resolveBlogTemplate(single).name());

        BlogVisualRequest duo = new BlogVisualRequest(
                "Article Tech", List.of(new PersonInput("Jean Dupont"), new PersonInput("Marie Curie")));
        assertEquals(
                TemplateService.DEFAULT_BLOG_DUO_TEMPLATE,
                templateService.resolveBlogTemplate(duo).name());
    }

    @Test
    void shouldRouteNightClazzTemplatesBasedOnPartnerPresence() {
        NightClazzVisualRequest standardNc = new NightClazzVisualRequest(
                "Soirée IA Générative",
                "19/11",
                "18h30-21h",
                "Zenika Nantes",
                List.of(new PersonInput("Jean Dupont"), new PersonInput("Marie Curie")));
        assertEquals(
                TemplateService.DEFAULT_NIGHTCLAZZ_TEMPLATE,
                templateService.resolveNightClazzTemplate(standardNc).name());

        NightClazzVisualRequest partnerNc = NightClazzVisualRequest.withPartner(
                "Soirée Cloud Native",
                "26/11",
                "18h30-21h",
                "Zenika Nantes",
                new PersonInput("Jean Dupont"),
                new PartnerInput("Google Cloud"));
        assertEquals(
                TemplateService.DEFAULT_NIGHTCLAZZ_PARTNER_TEMPLATE,
                templateService.resolveNightClazzTemplate(partnerNc).name());
    }

    @Test
    void shouldRejectInvalidSpeakerOrWriterCounts() {
        TalkVisualRequest noSpeakers = new TalkVisualRequest("Talk", "Devoxx", List.of());
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTalkTemplate(noSpeakers));

        TalkVisualRequest tooManySpeakers = new TalkVisualRequest(
                "Talk",
                "Devoxx",
                List.of(new PersonInput("A"), new PersonInput("B"), new PersonInput("C"), new PersonInput("D")));
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTalkTemplate(tooManySpeakers));

        BlogVisualRequest noWriters = new BlogVisualRequest("Blog", List.of());
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveBlogTemplate(noWriters));

        BlogVisualRequest tooManyWriters = new BlogVisualRequest(
                "Blog", List.of(new PersonInput("A"), new PersonInput("B"), new PersonInput("C")));
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveBlogTemplate(tooManyWriters));

        NightClazzVisualRequest partnerNcWithTwoSpeakers = new NightClazzVisualRequest(
                "NightClazz",
                "12/11",
                "18h30",
                "Zenika Nantes",
                List.of(new PersonInput("A"), new PersonInput("B")),
                new PartnerInput("Partenaire"));
        assertThrows(
                IllegalArgumentException.class,
                () -> templateService.resolveNightClazzTemplate(partnerNcWithTwoSpeakers));
    }

    @Test
    void shouldRejectTalkWithoutConference() {
        TalkVisualRequest missingConf =
                new TalkVisualRequest("Talk", (ConferenceInput) null, List.of(new PersonInput("Jean")));
        assertThrows(IllegalArgumentException.class, () -> templateService.resolveTalkTemplate(missingConf));
    }

    @Test
    void shouldBuildTalkBlogAndNightClazzPromptsWithAllPlaceholdersReplaced() {
        TalkVisualRequest talkRequest = new TalkVisualRequest(
                "Les agents IA avec Quarkus", "Sunny Tech 2026", List.of(new PersonInput("Jean-Phi", "photo-base64")));
        VisualTemplate talkTemplate = templateService.resolveTalkTemplate(talkRequest);
        String talkPrompt = templateService.buildTalkPrompt(talkTemplate, talkRequest);

        assertTrue(talkPrompt.contains("Les agents IA avec Quarkus"));
        assertTrue(talkPrompt.contains("Jean-Phi"));
        assertTrue(talkPrompt.contains("Sunny Tech 2026"));
        assertFalse(talkPrompt.contains("%TITLE%"));
        assertFalse(talkPrompt.contains("%NAME%"));
        assertFalse(talkPrompt.contains("%CONFERENCE%"));

        BlogVisualRequest blogRequest =
                new BlogVisualRequest("Découvrir Google GenAI en Java", List.of(new PersonInput("Jean-Phi")));
        VisualTemplate blogTemplate = templateService.resolveBlogTemplate(blogRequest);
        String blogPrompt = templateService.buildBlogPrompt(blogTemplate, blogRequest);

        assertTrue(blogPrompt.contains("Découvrir Google GenAI en Java"));
        assertTrue(blogPrompt.contains("Jean-Phi"));
        assertFalse(blogPrompt.contains("%TITLE%"));
        assertFalse(blogPrompt.contains("%NAME%"));

        NightClazzVisualRequest ncPartnerRequest = NightClazzVisualRequest.withPartner(
                "Agents IA en Production",
                "24/11",
                "18h30-21h",
                "Zenika Nantes",
                new PersonInput("Jean-Phi"),
                new PartnerInput("Google Cloud"));
        VisualTemplate ncPartnerTemplate = templateService.resolveNightClazzTemplate(ncPartnerRequest);
        String ncPartnerPrompt = templateService.buildNightClazzPrompt(ncPartnerTemplate, ncPartnerRequest);

        assertTrue(ncPartnerPrompt.contains("Agents IA en Production"));
        assertTrue(ncPartnerPrompt.contains("24/11"));
        assertTrue(ncPartnerPrompt.contains("Jean-Phi"));
        assertTrue(ncPartnerPrompt.contains("Google Cloud"));
        assertFalse(ncPartnerPrompt.contains("%DATE%"));
        assertFalse(ncPartnerPrompt.contains("%NAME%"));
        assertFalse(ncPartnerPrompt.contains("%PARTNER%"));
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
        Files.write(customTemplatesFolder.resolve("conf-1.png"), customBytes);

        TemplateService customService = new TemplateService(new ObjectMapper(), Optional.of(tempDir.toString()));
        InlineMediaPart externalMedia = customService.loadTemplateImage(talkTemplate);
        assertEquals(5, externalMedia.data().length);
    }
}
