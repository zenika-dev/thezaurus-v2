package com.zenika.thezaurus.visuals.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenika.thezaurus.visuals.client.GenAiVisualClient;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.GeneratedImage;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.BlogVisualRequest;
import com.zenika.thezaurus.visuals.model.ConferenceInput;
import com.zenika.thezaurus.visuals.model.NightClazzVisualRequest;
import com.zenika.thezaurus.visuals.model.PartnerInput;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.PhotoInput;
import com.zenika.thezaurus.visuals.model.TalkVisualRequest;
import com.zenika.thezaurus.visuals.model.VisualGenerationResponse;
import com.zenika.thezaurus.visuals.model.VisualStreamEvent;
import com.zenika.thezaurus.visuals.model.VisualTemplate;
import com.zenika.thezaurus.visuals.model.VisualType;
import com.zenika.thezaurus.visuals.service.VisualGenerationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests d'intégration Quarkus ({@link QuarkusTest}) des endpoints de {@link VisualResource}.
 */
@QuarkusTest
class VisualResourceTest {

    private static final byte[] FAKE_GENERATED_PNG = "fake-generated-image-bytes".getBytes(StandardCharsets.UTF_8);
    private static final String EXPECTED_BASE64 = Base64.getEncoder().encodeToString(FAKE_GENERATED_PNG);

    @Inject
    VisualResource visualResource;

    @InjectMock
    GenAiVisualClient genAiVisualClient;

    @BeforeEach
    void setUp() {
        when(genAiVisualClient.generateImage(anyString(), anyString(), anyList()))
                .thenReturn(new GeneratedImage(FAKE_GENERATED_PNG, "image/png"));
    }

    @Test
    void shouldListAvailableVisualTemplates() {
        List<VisualTemplate> allTemplates = visualResource.listTemplates(null);
        assertEquals(8, allTemplates.size());

        List<VisualTemplate> talkTemplates = visualResource.listTemplates(VisualType.TALK);
        assertEquals(4, talkTemplates.size());
        assertTrue(talkTemplates.stream().allMatch(t -> t.category() == VisualType.TALK));

        List<VisualTemplate> blogTemplates = visualResource.listTemplates(VisualType.BLOG);
        assertEquals(2, blogTemplates.size());
        assertTrue(blogTemplates.stream().allMatch(t -> t.category() == VisualType.BLOG));

        List<VisualTemplate> nightClazzTemplates = visualResource.listTemplates(VisualType.NIGHT_CLAZZ);
        assertEquals(2, nightClazzTemplates.size());
        assertTrue(nightClazzTemplates.stream().allMatch(t -> t.category() == VisualType.NIGHT_CLAZZ));
    }

    @Test
    void shouldGenerateTalkVisualUsingDynamicRoutingWithoutTemplateInput() {
        String validPngBase64 = Base64.getEncoder().encodeToString(VisualGenerationService.getDefaultPersonIconPng());

        TalkVisualRequest request = new TalkVisualRequest(
                "Architecture Hexagonale avec Quarkus",
                new ConferenceInput("Devoxx France 2026", validPngBase64),
                List.of(new PersonInput("Jean-Philippe Baconnais", PhotoInput.ofBase64(validPngBase64))));

        RestResponse<VisualGenerationResponse> restResponse = visualResource.generateTalkVisual(request);
        assertEquals(200, restResponse.getStatus());

        VisualGenerationResponse body = restResponse.getEntity();
        assertNotNull(body);
        assertEquals(VisualType.TALK, body.category());
        assertEquals("generate-image-speaker-event", body.templateName());
        assertEquals("gemini-2.5-flash-image", body.model());
        assertEquals("image/png", body.mimeType());
        assertEquals(EXPECTED_BASE64, body.imageBase64());
        assertTrue(body.promptUsed().contains("Architecture Hexagonale avec Quarkus"));
        assertTrue(body.promptUsed().contains("Jean-Philippe Baconnais"));
        assertTrue(body.promptUsed().contains("Devoxx France 2026"));
        assertNotNull(body.generatedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InlineMediaPart>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(genAiVisualClient)
                .generateImage(eq("gemini-2.5-flash-image"), promptCaptor.capture(), mediaCaptor.capture());
        assertEquals(3, mediaCaptor.getValue().size(), "Doit inclure le template + photo speaker + logo conférence");
        assertTrue(promptCaptor.getValue().contains("Devoxx France 2026"));
    }

    @Test
    void shouldGenerateBlogVisualUsingDefaultPersonIconWhenNoPhotoProvided() {
        BlogVisualRequest request = new BlogVisualRequest(
                "Créer des visuels avec Google GenAI et Quarkus", List.of(new PersonInput("Jean-Philippe Baconnais")));

        RestResponse<VisualGenerationResponse> restResponse = visualResource.generateBlogVisual(request);
        assertEquals(200, restResponse.getStatus());

        VisualGenerationResponse body = restResponse.getEntity();
        assertNotNull(body);
        assertEquals(VisualType.BLOG, body.category());
        assertEquals("generate-image-blog-post", body.templateName());
        assertEquals("gemini-2.5-flash-image", body.model());
        assertEquals("image/png", body.mimeType());
        assertEquals(EXPECTED_BASE64, body.imageBase64());
        assertTrue(body.promptUsed().contains("Créer des visuels avec Google GenAI et Quarkus"));
        assertTrue(body.promptUsed().contains("Jean-Philippe Baconnais"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InlineMediaPart>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        verify(genAiVisualClient).generateImage(eq("gemini-2.5-flash-image"), anyString(), mediaCaptor.capture());
        assertEquals(
                2,
                mediaCaptor.getValue().size(),
                "Doit inclure l'image du template de blog + l'icône de personne par défaut");
        assertArrayEquals(
                VisualGenerationService.getDefaultPersonIconPng(),
                mediaCaptor.getValue().get(1).data());
    }

    @Test
    void shouldGenerateNightClazzVisualUsingDynamicRouting() {
        String validPngBase64 = Base64.getEncoder().encodeToString(VisualGenerationService.getDefaultPersonIconPng());

        NightClazzVisualRequest request = NightClazzVisualRequest.withPartner(
                "Découvrir Gemini 4 avec Quarkus",
                "19/11",
                "18h30-21h",
                "Zenika Nantes",
                new PersonInput("Jean-Philippe Baconnais", PhotoInput.ofBase64(validPngBase64)),
                new PartnerInput("Google Cloud", validPngBase64));

        RestResponse<VisualGenerationResponse> restResponse = visualResource.generateNightClazzVisual(request);
        assertEquals(200, restResponse.getStatus());

        VisualGenerationResponse body = restResponse.getEntity();
        assertNotNull(body);
        assertEquals(VisualType.NIGHT_CLAZZ, body.category());
        assertEquals("generate-image-nightclazz-partner", body.templateName());
        assertEquals(EXPECTED_BASE64, body.imageBase64());
        assertTrue(body.promptUsed().contains("Découvrir Gemini 4 avec Quarkus"));
        assertTrue(body.promptUsed().contains("Google Cloud"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InlineMediaPart>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        verify(genAiVisualClient).generateImage(eq("gemini-2.5-flash-image"), anyString(), mediaCaptor.capture());
        assertEquals(3, mediaCaptor.getValue().size(), "Doit inclure le template + photo speaker + logo partenaire");
    }

    @Test
    void shouldReturnBinaryPngImageDirectlyOnImageEndpoints() {
        BlogVisualRequest blogRequest =
                new BlogVisualRequest("Visualisation directe PNG", List.of(new PersonInput("Jean-Phi")));

        RestResponse<byte[]> imageResponse = visualResource.generateBlogVisualImage(blogRequest);
        assertEquals(200, imageResponse.getStatus());
        assertArrayEquals(FAKE_GENERATED_PNG, imageResponse.getEntity());
        assertEquals("generate-image-blog-post", imageResponse.getHeaderString("X-Visual-Template"));
    }

    @Test
    void shouldRejectInvalidRequestsViaBeanValidation() {
        TalkVisualRequest invalidTalkRequest = new TalkVisualRequest("", (ConferenceInput) null, null);
        assertThrows(ConstraintViolationException.class, () -> visualResource.generateTalkVisual(invalidTalkRequest));

        BlogVisualRequest invalidBlogRequest = new BlogVisualRequest("", List.of());
        assertThrows(ConstraintViolationException.class, () -> visualResource.generateBlogVisual(invalidBlogRequest));
    }

    @Test
    void shouldStreamVisualGenerationViaSse() {
        TalkVisualRequest talkRequest =
                new TalkVisualRequest("SSE vs REST avec Quarkus", "Riviera DEV", List.of(new PersonInput("Jean-Phi")));

        List<VisualStreamEvent> talkEvents = visualResource
                .streamTalkVisual(talkRequest)
                .collect()
                .asList()
                .await()
                .atMost(Duration.ofSeconds(5));

        assertTrue(talkEvents.stream().anyMatch(e -> e.status() == VisualStreamEvent.Status.STARTED));
        assertTrue(talkEvents.stream().anyMatch(e -> e.status() == VisualStreamEvent.Status.TEMPLATE_LOADED));
        assertTrue(talkEvents.stream().anyMatch(e -> e.status() == VisualStreamEvent.Status.GENERATING));
        VisualStreamEvent completedTalk = talkEvents.get(talkEvents.size() - 1);
        assertEquals(EXPECTED_BASE64, completedTalk.result().imageBase64());
    }

    @Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void shouldDeserializeFlexibleJsonInputs() throws Exception {
        String talkJson = """
                {
                    "title": "Architecture Hexagonale",
                    "conference": "Devoxx France 2026",
                    "speakers": ["Jean-Philippe Baconnais", "Marie Curie"]
                }
                """;

        TalkVisualRequest req1 = objectMapper.readValue(talkJson, TalkVisualRequest.class);
        assertEquals("Architecture Hexagonale", req1.title());
        assertNotNull(req1.conference());
        assertEquals("Devoxx France 2026", req1.conference().name());
        assertEquals(2, req1.speakers().size());
        assertEquals("Jean-Philippe Baconnais", req1.speakers().get(0).name());
        assertEquals("Marie Curie", req1.speakers().get(1).name());

        String blogJson = """
                {
                    "title": "Article Tech",
                    "writers": [
                        {
                            "name": "Jean-Phi",
                            "photo": {
                                "url": "https://example.com/jean-phi.png",
                                "base64": "fake-photo-base64"
                            }
                        }
                    ]
                }
                """;

        BlogVisualRequest req2 = objectMapper.readValue(blogJson, BlogVisualRequest.class);
        assertEquals("Article Tech", req2.title());
        assertEquals(1, req2.writers().size());
        assertEquals("Jean-Phi", req2.writers().get(0).name());
        assertNotNull(req2.writers().get(0).photo());
        assertEquals(
                "https://example.com/jean-phi.png",
                req2.writers().get(0).photo().url());
        assertEquals("fake-photo-base64", req2.writers().get(0).photo().base64());

        String nightClazzJson = """
                {
                    "title": "Soirée IA",
                    "date": "19/11",
                    "hour": "18h30-21h",
                    "location": "Zenika Nantes",
                    "speakers": [
                        {
                            "name": "Jean-Phi",
                            "photo": {
                                "url": "string",
                                "base64": "string"
                            }
                        }
                    ],
                    "partner": "Google Cloud"
                }
                """;

        NightClazzVisualRequest req3 = objectMapper.readValue(nightClazzJson, NightClazzVisualRequest.class);
        assertEquals("Soirée IA", req3.title());
        assertEquals("19/11", req3.date());
        assertEquals(1, req3.speakers().size());
        assertEquals("Jean-Phi", req3.speakers().get(0).name());
        assertNull(req3.speakers().get(0).photo(), "Les valeurs placeholder 'string' doivent être ignorées");
        assertNotNull(req3.partner());
        assertEquals("Google Cloud", req3.partner().name());
    }
}
