package com.zenika.thezaurus.visuals.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.zenika.thezaurus.visuals.model.ConferenceInput;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.VisualGenerationResponse;
import com.zenika.thezaurus.visuals.model.VisualRequest;
import com.zenika.thezaurus.visuals.model.VisualStreamEvent;
import com.zenika.thezaurus.visuals.model.VisualTemplate;
import com.zenika.thezaurus.visuals.model.VisualType;
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
        assertTrue(allTemplates.size() >= 5);

        List<VisualTemplate> talkTemplates = visualResource.listTemplates(VisualType.TALK);
        assertEquals(3, talkTemplates.size());
        assertTrue(talkTemplates.stream().allMatch(t -> t.category() == VisualType.TALK));

        List<VisualTemplate> blogTemplates = visualResource.listTemplates(VisualType.BLOG);
        assertEquals(2, blogTemplates.size());
        assertTrue(blogTemplates.stream().allMatch(t -> t.category() == VisualType.BLOG));
    }

    @Test
    void shouldGenerateTalkVisualUsingDynamicRoutingWithoutTemplateInput() {
        String fakeSpeakerPhoto = Base64.getEncoder().encodeToString("speaker-photo".getBytes(StandardCharsets.UTF_8));
        String fakeConfLogo = Base64.getEncoder().encodeToString("conf-logo".getBytes(StandardCharsets.UTF_8));

        VisualRequest request = new VisualRequest(
                "Architecture Hexagonale avec Quarkus",
                "talk",
                new ConferenceInput("Devoxx France 2026", fakeConfLogo),
                List.of(new PersonInput("Jean-Philippe Baconnais", fakeSpeakerPhoto)),
                null);

        RestResponse<VisualGenerationResponse> restResponse = visualResource.generateVisual(request);
        assertEquals(200, restResponse.getStatus());

        VisualGenerationResponse body = restResponse.getEntity();
        assertNotNull(body);
        assertEquals(VisualType.TALK, body.category());
        assertEquals("generate-image-speaker-event", body.templateName());
        assertEquals("gemini-4-argon", body.model());
        assertEquals("image/png", body.mimeType());
        assertEquals(EXPECTED_BASE64, body.imageBase64());
        assertTrue(body.promptUsed().contains("Architecture Hexagonale avec Quarkus"));
        assertTrue(body.promptUsed().contains("Jean-Philippe Baconnais"));
        assertTrue(body.promptUsed().contains("Devoxx France 2026"));
        assertNotNull(body.generatedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InlineMediaPart>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(genAiVisualClient).generateImage(eq("gemini-4-argon"), promptCaptor.capture(), mediaCaptor.capture());
        assertEquals(3, mediaCaptor.getValue().size(), "Doit inclure le template + photo speaker + logo conférence");
        assertTrue(promptCaptor.getValue().contains("Devoxx France 2026"));
    }

    @Test
    void shouldGenerateBlogVisualUsingDynamicRoutingWithoutTemplateInput() {
        VisualRequest request = VisualRequest.forBlog(
                "Créer des visuels avec Google GenAI et Quarkus", List.of(new PersonInput("Jean-Philippe Baconnais")));

        RestResponse<VisualGenerationResponse> restResponse = visualResource.generateBlogVisual(request);
        assertEquals(200, restResponse.getStatus());

        VisualGenerationResponse body = restResponse.getEntity();
        assertNotNull(body);
        assertEquals(VisualType.BLOG, body.category());
        assertEquals("generate-image-blog-post", body.templateName());
        assertEquals("gemini-4-argon", body.model());
        assertEquals("image/png", body.mimeType());
        assertEquals(EXPECTED_BASE64, body.imageBase64());
        assertTrue(body.promptUsed().contains("Créer des visuels avec Google GenAI et Quarkus"));
        assertTrue(body.promptUsed().contains("Jean-Philippe Baconnais"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InlineMediaPart>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        verify(genAiVisualClient).generateImage(eq("gemini-4-argon"), anyString(), mediaCaptor.capture());
        assertEquals(1, mediaCaptor.getValue().size(), "Doit inclure l'image du template de blog");
    }

    @Test
    void shouldRejectInvalidRequestsViaBeanValidation() {
        VisualRequest invalidRequest = new VisualRequest("", "talk", null, null, null);
        assertThrows(ConstraintViolationException.class, () -> visualResource.generateVisual(invalidRequest));
    }

    @Test
    void shouldStreamVisualGenerationViaSse() {
        VisualRequest talkRequest =
                VisualRequest.forTalk("SSE vs REST avec Quarkus", "Riviera DEV", List.of(new PersonInput("Jean-Phi")));

        List<VisualStreamEvent> talkEvents = visualResource
                .streamVisual(talkRequest)
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
        String jsonWithStrings = """
                {
                    "title": "Architecture Hexagonale",
                    "type": "talk",
                    "conference": "Devoxx France 2026",
                    "speakers": ["Jean-Philippe Baconnais", "Marie Curie"]
                }
                """;

        VisualRequest req1 = objectMapper.readValue(jsonWithStrings, VisualRequest.class);
        assertEquals("Architecture Hexagonale", req1.title());
        assertEquals("talk", req1.type());
        assertNotNull(req1.conference());
        assertEquals("Devoxx France 2026", req1.conference().name());
        assertEquals(2, req1.speakers().size());
        assertEquals("Jean-Philippe Baconnais", req1.speakers().get(0).name());
        assertEquals("Marie Curie", req1.speakers().get(1).name());

        String jsonWithObjects = """
                {
                    "title": "Article Tech",
                    "type": "blog",
                    "writers": [
                        { "name": "Jean-Phi", "photoBase64": "fake-photo-base64" }
                    ]
                }
                """;

        VisualRequest req2 = objectMapper.readValue(jsonWithObjects, VisualRequest.class);
        assertEquals("Article Tech", req2.title());
        assertEquals("blog", req2.type());
        assertEquals(1, req2.writers().size());
        assertEquals("Jean-Phi", req2.writers().get(0).name());
        assertEquals("fake-photo-base64", req2.writers().get(0).photoBase64());
    }
}
