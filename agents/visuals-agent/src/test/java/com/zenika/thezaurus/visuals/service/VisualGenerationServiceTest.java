package com.zenika.thezaurus.visuals.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.GeneratedImage;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.model.ConferenceInput;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.VisualGenerationResponse;
import com.zenika.thezaurus.visuals.model.VisualRequest;
import com.zenika.thezaurus.visuals.model.VisualStreamEvent;
import com.zenika.thezaurus.visuals.model.VisualType;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VisualGenerationServiceTest {

    private static final byte[] FAKE_PNG = "fake-png-content".getBytes(StandardCharsets.UTF_8);
    private static final String FAKE_BASE64 = Base64.getEncoder().encodeToString(FAKE_PNG);

    private GenAiVisualClient genAiVisualClient;
    private VisualGenerationService service;

    @BeforeEach
    void setUp() {
        TemplateService templateService = new TemplateService(new ObjectMapper(), Optional.empty());
        genAiVisualClient = mock(GenAiVisualClient.class);
        when(genAiVisualClient.generateImage(anyString(), anyString(), anyList()))
                .thenReturn(new GeneratedImage(FAKE_PNG, "image/png"));
        service = new VisualGenerationService(templateService, genAiVisualClient, "gemini-4-argon", 50L);
    }

    @Test
    void shouldGenerateTalkVisualWithSpeakerAndConferenceAndTitle() {
        VisualRequest request = VisualRequest.forTalk(
                "Quarkus & Gemini", "Devoxx France", List.of(new PersonInput("Jean-Philippe Baconnais")));

        VisualGenerationResponse response = service.generateVisual(request);

        assertEquals(VisualType.TALK, response.category());
        assertEquals("generate-image-speaker-event", response.templateName());
        assertEquals("gemini-4-argon", response.model());
        assertEquals("image/png", response.mimeType());
        assertEquals(FAKE_BASE64, response.imageBase64());
        assertTrue(response.promptUsed().contains("Quarkus & Gemini"));
        assertTrue(response.promptUsed().contains("Jean-Philippe Baconnais"));
        assertTrue(response.promptUsed().contains("Devoxx France"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InlineMediaPart>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        verify(genAiVisualClient).generateImage(eq("gemini-4-argon"), anyString(), mediaCaptor.capture());
        assertEquals(1, mediaCaptor.getValue().size());
    }

    @Test
    void shouldGenerateBlogVisualWithAuthorAndTitle() {
        VisualRequest request =
                VisualRequest.forBlog("Les nouveautés de Java 21", List.of(new PersonInput("Jean-Philippe Baconnais")));

        VisualGenerationResponse response = service.generateVisual(request);

        assertEquals(VisualType.BLOG, response.category());
        assertEquals("generate-image-blog-post", response.templateName());
        assertEquals("gemini-4-argon", response.model());
        assertEquals("image/png", response.mimeType());
        assertEquals(FAKE_BASE64, response.imageBase64());
        assertTrue(response.promptUsed().contains("Les nouveautés de Java 21"));
        assertTrue(response.promptUsed().contains("Jean-Philippe Baconnais"));
    }

    @Test
    void shouldValidateMandatoryFieldsOnRequests() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.generateVisual(new VisualRequest(
                        "", "talk", new ConferenceInput("Conf"), List.of(new PersonInput("Speaker")), null)));
        assertThrows(IllegalArgumentException.class, () -> service.generateVisual(null));
    }

    @Test
    void shouldEmitSseEventsIncludingHeartbeatsDuringSlowGeneration() {
        when(genAiVisualClient.generateImage(anyString(), anyString(), anyList()))
                .thenAnswer(inv -> {
                    Thread.sleep(130L);
                    return new GeneratedImage(FAKE_PNG, "image/png");
                });

        VisualRequest request =
                VisualRequest.forTalk("Streaming SSE", "Camping des Speakers", List.of(new PersonInput("Jean-Phi")));

        List<VisualStreamEvent> events =
                service.streamVisual(request).collect().asList().await().atMost(Duration.ofSeconds(5));

        assertTrue(events.size() >= 5, "Doit contenir STARTED, TEMPLATE_LOADED, GENERATING, HEARTBEAT(s), COMPLETED");
        assertEquals(VisualStreamEvent.Status.STARTED, events.get(0).status());
        assertEquals(VisualStreamEvent.Status.TEMPLATE_LOADED, events.get(1).status());
        assertEquals(VisualStreamEvent.Status.GENERATING, events.get(2).status());
        assertTrue(events.stream().anyMatch(e -> e.status() == VisualStreamEvent.Status.HEARTBEAT));
        VisualStreamEvent last = events.get(events.size() - 1);
        assertEquals(VisualStreamEvent.Status.COMPLETED, last.status());
        assertNotNull(last.result());
        assertEquals(FAKE_BASE64, last.result().imageBase64());
    }
}
