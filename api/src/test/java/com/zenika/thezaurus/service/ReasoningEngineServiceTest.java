/*package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ReasoningEngineServiceTest {

    @Inject
    ReasoningEngineService service;

    @Test
    public void testReviewTalkWithNullRequest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.reviewTalk(null);
        });
    }

    @Test
    public void testReviewTalkWithNullTitle() {
        TalkReviewRequest req = new TalkReviewRequest(null, "Abstract de test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.reviewTalk(req);
        });
    }

    @Test
    public void testParseSseResponseWithImprovementsStructure() {
        String ssePayload = """
                data: {"improvements": [
                    {"category": "titre", "comment": "Titre très accrocheur.", "suggestions": ["Titre Suggéré 1", "Titre Suggéré 2"]},
                    {"category": "abstract", "comment": "Abstract bien structuré.", "suggestions": ["Abstract Suggéré 1"]},
                    {"category": "coherence", "comment": "Très bonne cohérence.", "suggestions": ["Recommandation cohérence 1"]}
                ]}
                """;

        TalkReviewResponse response = service.parseSseResponse(ssePayload);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(2, response.suggestedTitles().size());
        Assertions.assertEquals("Titre Suggéré 1", response.suggestedTitles().get(0));
        Assertions.assertEquals("Titre Suggéré 2", response.suggestedTitles().get(1));

        Assertions.assertEquals(1, response.suggestedAbstracts().size());
        Assertions.assertEquals("Abstract Suggéré 1", response.suggestedAbstracts().getFirst());

        Assertions.assertEquals(3, response.feedback().size());
        Assertions.assertTrue(response.feedback().get(0).contains("[Titre]"));
        Assertions.assertTrue(response.feedback().get(1).contains("[Abstract]"));
        Assertions.assertTrue(response.feedback().get(2).contains("[Cohérence]"));

        Assertions.assertEquals(1, response.keyImprovements().size());
        Assertions.assertEquals("Recommandation cohérence 1", response.keyImprovements().getFirst());
    }

    @Test
    public void testParseSseResponseWithGeminiPartsWrapper() {
        String ssePayload = """
                data: {"content": {"parts": [{"text": "{\\"improvements\\": [{\\"category\\": \\"titre\\", \\"comment\\": \\"Titre OK\\", \\"suggestions\\": [\\"Titre IA 1\\"]}]}"}]}}
                """;

        TalkReviewResponse response = service.parseSseResponse(ssePayload);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.suggestedTitles().size());
        Assertions.assertEquals("Titre IA 1", response.suggestedTitles().getFirst());
        Assertions.assertEquals(1, response.feedback().size());
    }

    @Test
    public void testParseSseResponseWithDoneAndInvalidLines() {
        String ssePayload = """
                data: [DONE]

                data: invalid-json
                data: {"improvements": [{"category": "titre", "comment": "Super", "suggestions": ["Super Titre"]}]}
                """;

        TalkReviewResponse response = service.parseSseResponse(ssePayload);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.suggestedTitles().size());
        Assertions.assertEquals("Super Titre", response.suggestedTitles().getFirst());
    }

    @Test
    public void testParseSseResponseWithRawJson() {
        String rawJson = """
                {"improvements": [{"category": "titre", "comment": "Avis", "suggestions": ["Titre Brut"]}]}
                """;

        TalkReviewResponse response = service.parseSseResponse(rawJson);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.suggestedTitles().size());
        Assertions.assertEquals("Titre Brut", response.suggestedTitles().getFirst());
    }

    @Test
    public void testParseSseResponseNullOrBlank() {
        Assertions.assertNull(service.parseSseResponse(null));
        Assertions.assertNull(service.parseSseResponse("   "));
    }

    @Test
    public void testReviewTalkThrowsExceptionWhenNetworkFails() {
        ReasoningEngineService customService = new ReasoningEngineService();
        customService.reasoningEngineUrl = "https://unreachable-host-name-123456.example.com";

        TalkReviewRequest req = new TalkReviewRequest("Mets du Front dans ton back", "Description de la présentation");

        Assertions.assertThrows(RuntimeException.class, () -> customService.reviewTalk(req));
    }
}
*/