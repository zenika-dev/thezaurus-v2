package com.zenika.thezaurus.service;

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
        TalkReviewRequest req = new TalkReviewRequest();
        req.setTitle(null);
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

        TalkReviewResponse response = service.parseSseResponse(ssePayload, "Mon Titre", "Mon Abstract");

        Assertions.assertNotNull(response);
        Assertions.assertEquals(2, response.getSuggestedTitles().size());
        Assertions.assertEquals("Titre Suggéré 1", response.getSuggestedTitles().get(0));
        Assertions.assertEquals("Titre Suggéré 2", response.getSuggestedTitles().get(1));

        Assertions.assertEquals(1, response.getSuggestedAbstracts().size());
        Assertions.assertEquals("Abstract Suggéré 1", response.getSuggestedAbstracts().get(0));

        Assertions.assertEquals(3, response.getFeedback().size());
        Assertions.assertTrue(response.getFeedback().get(0).contains("[Titre]"));
        Assertions.assertTrue(response.getFeedback().get(1).contains("[Abstract]"));
        Assertions.assertTrue(response.getFeedback().get(2).contains("[Cohérence]"));

        Assertions.assertEquals(1, response.getKeyImprovements().size());
        Assertions.assertEquals("Recommandation cohérence 1", response.getKeyImprovements().get(0));
    }

    @Test
    public void testParseSseResponseWithGeminiPartsWrapper() {
        String ssePayload = """
                data: {"content": {"parts": [{"text": "{\\"improvements\\": [{\\"category\\": \\"titre\\", \\"comment\\": \\"Titre OK\\", \\"suggestions\\": [\\"Titre IA 1\\"]}]}"}]}}
                """;

        TalkReviewResponse response = service.parseSseResponse(ssePayload, "Titre Initial", "Abstract Initial");

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getSuggestedTitles().size());
        Assertions.assertEquals("Titre IA 1", response.getSuggestedTitles().get(0));
        Assertions.assertEquals(1, response.getFeedback().size());
    }

    @Test
    public void testParseSseResponseWithDoneAndInvalidLines() {
        String ssePayload = """
                data: [DONE]

                data: invalid-json
                data: {"improvements": [{"category": "titre", "comment": "Super", "suggestions": ["Super Titre"]}]}
                """;

        TalkReviewResponse response = service.parseSseResponse(ssePayload, "Titre", "Abstract");

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getSuggestedTitles().size());
        Assertions.assertEquals("Super Titre", response.getSuggestedTitles().get(0));
    }

    @Test
    public void testParseSseResponseWithRawJson() {
        String rawJson = """
                {"improvements": [{"category": "titre", "comment": "Avis", "suggestions": ["Titre Brut"]}]}
                """;

        TalkReviewResponse response = service.parseSseResponse(rawJson, "Titre", "Abstract");

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getSuggestedTitles().size());
        Assertions.assertEquals("Titre Brut", response.getSuggestedTitles().get(0));
    }

    @Test
    public void testParseSseResponseNullOrBlank() {
        Assertions.assertNull(service.parseSseResponse(null, "Titre", "Abstract"));
        Assertions.assertNull(service.parseSseResponse("   ", "Titre", "Abstract"));
    }

    @Test
    public void testReviewTalkFallbackWhenNetworkFails() {
        TalkReviewRequest req = new TalkReviewRequest("Mets du Front dans ton back", "Description de la présentation");
        TalkReviewResponse response = service.reviewTalk(req);

        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.getSuggestedTitles().isEmpty());
        Assertions.assertFalse(response.getSuggestedAbstracts().isEmpty());
        Assertions.assertFalse(response.getFeedback().isEmpty());
    }
}
