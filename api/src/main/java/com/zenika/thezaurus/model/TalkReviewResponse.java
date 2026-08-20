package com.zenika.thezaurus.model;

import java.util.List;

public class TalkReviewResponse {
    private List<String> suggestedTitles;
    private List<String> suggestedAbstracts;
    private List<String> feedback;
    private List<String> keyImprovements;

    public TalkReviewResponse() {
    }

    public TalkReviewResponse(List<String> suggestedTitles, List<String> suggestedAbstracts, List<String> feedback, List<String> keyImprovements) {
        this.suggestedTitles = suggestedTitles;
        this.suggestedAbstracts = suggestedAbstracts;
        this.feedback = feedback;
        this.keyImprovements = keyImprovements;
    }

    public List<String> getSuggestedTitles() {
        return suggestedTitles;
    }

    public void setSuggestedTitles(List<String> suggestedTitles) {
        this.suggestedTitles = suggestedTitles;
    }

    public List<String> getSuggestedAbstracts() {
        return suggestedAbstracts;
    }

    public void setSuggestedAbstracts(List<String> suggestedAbstracts) {
        this.suggestedAbstracts = suggestedAbstracts;
    }

    public List<String> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<String> feedback) {
        this.feedback = feedback;
    }

    public List<String> getKeyImprovements() {
        return keyImprovements;
    }

    public void setKeyImprovements(List<String> keyImprovements) {
        this.keyImprovements = keyImprovements;
    }
}
