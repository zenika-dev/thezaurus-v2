package com.zenika.thezaurus.visuals.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.genai.types.Blob;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.GeneratedImage;
import com.zenika.thezaurus.visuals.client.GenAiVisualClient.InlineMediaPart;
import com.zenika.thezaurus.visuals.exception.VisualGenerationException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires du client {@link GoogleGenAiVisualClient} sans aucun appel réseau ni consommation d'IA.
 */
class GoogleGenAiVisualClientTest {

    private final GoogleGenAiVisualClient client = new GoogleGenAiVisualClient(
            Optional.of("fake-api-key-no-network"), false, Optional.empty(), "europe-west1", 30000);

    @Test
    void shouldBuildMultimodalContentWithPromptAndTemplateBytes() {
        byte[] templateBytes = "fake-template-png".getBytes(StandardCharsets.UTF_8);
        byte[] speakerBytes = "fake-speaker-jpg".getBytes(StandardCharsets.UTF_8);

        Content content = client.buildContent(
                "Generate talk visual",
                List.of(
                        new InlineMediaPart(templateBytes, "image/png"),
                        new InlineMediaPart(speakerBytes, "image/jpeg")));

        assertNotNull(content);
        assertTrue(content.parts().isPresent());
        List<Part> parts = content.parts().get();
        assertEquals(3, parts.size());
        assertEquals("Generate talk visual", parts.get(0).text().orElseThrow());
        assertArrayEquals(
                templateBytes, parts.get(1).inlineData().orElseThrow().data().orElseThrow());
        assertEquals(
                "image/png", parts.get(1).inlineData().orElseThrow().mimeType().orElseThrow());
        assertArrayEquals(
                speakerBytes, parts.get(2).inlineData().orElseThrow().data().orElseThrow());
        assertEquals(
                "image/jpeg", parts.get(2).inlineData().orElseThrow().mimeType().orElseThrow());
    }

    @Test
    void shouldExtractGeneratedImageFromGenAiResponse() {
        byte[] expectedPng = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
        Part textPart = Part.fromText("Here is the generated visual");
        Part imagePart = Part.builder()
                .inlineData(
                        Blob.builder().data(expectedPng).mimeType("image/png").build())
                .build();

        Candidate candidate = Candidate.builder()
                .content(Content.fromParts(textPart, imagePart))
                .finishReason(new FinishReason(FinishReason.Known.STOP))
                .build();

        GenerateContentResponse fakeResponse =
                GenerateContentResponse.builder().candidates(List.of(candidate)).build();

        GeneratedImage extracted = client.extractImage(fakeResponse);
        assertNotNull(extracted);
        assertEquals("image/png", extracted.mimeType());
        assertArrayEquals(expectedPng, extracted.data());
    }

    @Test
    void shouldThrowExceptionWhenResponseContainsNoImagePart() {
        Candidate textOnlyCandidate = Candidate.builder()
                .content(Content.fromParts(Part.fromText("No image generated")))
                .finishReason(new FinishReason(FinishReason.Known.STOP))
                .build();

        GenerateContentResponse fakeResponse = GenerateContentResponse.builder()
                .candidates(List.of(textOnlyCandidate))
                .build();

        assertThrows(VisualGenerationException.class, () -> client.extractImage(fakeResponse));
    }
}
