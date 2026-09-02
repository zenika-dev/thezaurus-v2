package com.zenika.thezaurus.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenika.thezaurus.client.TalkReviewAdapter;
import com.zenika.thezaurus.exception.TalkReviewException;
import com.zenika.thezaurus.mapper.TalkReviewMapper;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TalkReviewServiceTest {

    @Mock
    private TalkReviewAdapter talkReviewAdapter;

    @Mock
    private TalkReviewMapper talkReviewMapper;

    @InjectMocks
    private TalkReviewService talkReviewService;

    @Test
    @DisplayName("reviewTalk - avec requête nulle - lève IllegalArgumentException")
    void reviewTalk_WithNullRequest_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> talkReviewService.reviewTalk(null));

        Assertions.assertEquals("Le titre du talk est requis.", exception.getMessage());
        verify(talkReviewAdapter, never()).sendStreamQuery(anyString(), anyString());
    }

    @Test
    @DisplayName("reviewTalk - avec titre nul - lève IllegalArgumentException")
    void reviewTalk_WithNullTitle_ThrowsIllegalArgumentException() {
        TalkReviewRequest request = new TalkReviewRequest(null, "Abstract valide");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> talkReviewService.reviewTalk(request));

        Assertions.assertEquals("Le titre du talk est requis.", exception.getMessage());
        verify(talkReviewAdapter, never()).sendStreamQuery(anyString(), anyString());
    }

    @Test
    @DisplayName("reviewTalk - avec titre vide ou constitué d'espaces - lève IllegalArgumentException")
    void reviewTalk_WithBlankTitle_ThrowsIllegalArgumentException() {
        TalkReviewRequest request = new TalkReviewRequest("   ", "Abstract valide");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> talkReviewService.reviewTalk(request));

        Assertions.assertEquals("Le titre du talk est requis.", exception.getMessage());
        verify(talkReviewAdapter, never()).sendStreamQuery(anyString(), anyString());
    }

    @Test
    @DisplayName("reviewTalk - échec de l'adaptateur HTTP - lève TalkReviewException")
    void reviewTalk_AdapterReturnsEmpty_ThrowsTalkReviewException() {
        TalkReviewRequest request = new TalkReviewRequest("Titre de talk", "Abstract de talk");

        when(talkReviewAdapter.sendStreamQuery("Titre de talk", "Abstract de talk"))
                .thenReturn(Optional.empty());

        TalkReviewException exception =
                Assertions.assertThrows(TalkReviewException.class, () -> talkReviewService.reviewTalk(request));

        Assertions.assertEquals(
                "Impossible d'obtenir la revue du talk auprès du Reasoning Engine AI Agent.", exception.getMessage());
        verify(talkReviewAdapter).sendStreamQuery("Titre de talk", "Abstract de talk");
        verify(talkReviewMapper, never()).toDomain(anyString());
    }

    @Test
    @DisplayName("reviewTalk - échec du mapper JSON - lève TalkReviewException")
    void reviewTalk_MapperReturnsEmpty_ThrowsTalkReviewException() {
        TalkReviewRequest request = new TalkReviewRequest("Titre de talk", "Abstract de talk");
        String rawSseResponse = "data: {\"invalid\": true}";

        when(talkReviewAdapter.sendStreamQuery("Titre de talk", "Abstract de talk"))
                .thenReturn(Optional.of(rawSseResponse));
        when(talkReviewMapper.toDomain(rawSseResponse)).thenReturn(Optional.empty());

        TalkReviewException exception =
                Assertions.assertThrows(TalkReviewException.class, () -> talkReviewService.reviewTalk(request));

        Assertions.assertEquals(
                "Impossible d'obtenir la revue du talk auprès du Reasoning Engine AI Agent.", exception.getMessage());
        verify(talkReviewAdapter).sendStreamQuery("Titre de talk", "Abstract de talk");
        verify(talkReviewMapper).toDomain(rawSseResponse);
    }

    @Test
    @DisplayName("reviewTalk - succès complet - retourne TalkReviewResponse")
    void reviewTalk_Success_ReturnsTalkReviewResponse() {
        TalkReviewRequest request = new TalkReviewRequest("  Titre de talk  ", "  Abstract de talk  ");
        String rawSseResponse = "data: {\"improvements\": []}";

        TalkReviewResponse expectedResponse = new TalkReviewResponse(
                List.of("Titre 1", "Titre 2"),
                List.of("Abstract 1"),
                List.of("[Titre] Bon titre"),
                List.of("Amélioration 1"));

        when(talkReviewAdapter.sendStreamQuery("Titre de talk", "Abstract de talk"))
                .thenReturn(Optional.of(rawSseResponse));
        when(talkReviewMapper.toDomain(rawSseResponse)).thenReturn(Optional.of(expectedResponse));

        TalkReviewResponse actualResponse = talkReviewService.reviewTalk(request);

        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(expectedResponse, actualResponse);
        Assertions.assertEquals(2, actualResponse.suggestedTitles().size());
        Assertions.assertEquals("Titre 1", actualResponse.suggestedTitles().get(0));

        verify(talkReviewAdapter).sendStreamQuery("Titre de talk", "Abstract de talk");
        verify(talkReviewMapper).toDomain(rawSseResponse);
    }

    @Test
    @DisplayName("reviewTalk - avec abstract nul - transmet chaîne vide à l'adaptateur")
    void reviewTalk_WithNullAbstract_PassesEmptyStringToAdapter() {
        TalkReviewRequest request = new TalkReviewRequest("Titre de talk", null);
        String rawSseResponse = "data: {}";
        TalkReviewResponse expectedResponse =
                new TalkReviewResponse(List.of("Titre 1"), List.of(), List.of(), List.of());

        when(talkReviewAdapter.sendStreamQuery("Titre de talk", "")).thenReturn(Optional.of(rawSseResponse));
        when(talkReviewMapper.toDomain(rawSseResponse)).thenReturn(Optional.of(expectedResponse));

        TalkReviewResponse actualResponse = talkReviewService.reviewTalk(request);

        Assertions.assertNotNull(actualResponse);
        verify(talkReviewAdapter).sendStreamQuery("Titre de talk", "");
    }
}
