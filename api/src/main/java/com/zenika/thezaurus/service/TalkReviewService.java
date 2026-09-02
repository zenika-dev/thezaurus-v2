package com.zenika.thezaurus.service;

import com.zenika.thezaurus.client.TalkReviewAdapter;
import com.zenika.thezaurus.exception.TalkReviewException;
import com.zenika.thezaurus.mapper.TalkReviewMapper;
import com.zenika.thezaurus.model.TalkReviewRequest;
import com.zenika.thezaurus.model.TalkReviewResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TalkReviewService {

    @Inject
    TalkReviewAdapter talkReviewAdapter;

    @Inject
    TalkReviewMapper talkReviewMapper;

    public TalkReviewResponse reviewTalk(TalkReviewRequest request) {
        validateRequest(request);

        String title = request.title().trim();
        String abstractText =
                request.abstractText() != null ? request.abstractText().trim() : "";

        return talkReviewAdapter
                .sendStreamQuery(title, abstractText)
                .flatMap(talkReviewMapper::toDomain)
                .orElseThrow(() -> new TalkReviewException(
                        "Impossible d'obtenir la revue du talk auprès du Reasoning Engine AI Agent."));
    }

    private void validateRequest(TalkReviewRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Le titre du talk est requis.");
        }
    }
}
