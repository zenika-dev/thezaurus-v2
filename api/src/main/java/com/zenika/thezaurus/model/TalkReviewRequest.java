package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TalkReviewRequest {
    private String title;
    @JsonProperty("abstract")
    private String abstractText;
    private String format;
    private String language;

    public TalkReviewRequest() {
    }

    public TalkReviewRequest(String title, String abstractText) {
        this(title, abstractText, null, null);
    }

    public TalkReviewRequest(String title, String abstractText, String format, String language) {
        this.title = title;
        this.abstractText = abstractText;
        this.format = format;
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
