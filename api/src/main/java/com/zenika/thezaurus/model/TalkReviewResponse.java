package com.zenika.thezaurus.model;

import java.util.List;

public record TalkReviewResponse(
        List<String> suggestedTitles,
        List<String> suggestedAbstracts,
        List<String> feedback,
        List<String> keyImprovements) {}
