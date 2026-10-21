package com.zenika.thezaurus.exception;

import jakarta.ws.rs.core.Response;

public class TalkReviewException extends ThezaurusException {

    public static final String REVIEW_UNAVAILABLE_MESSAGE =
            "L'assistant IA rencontre une difficulté temporaire. Merci de réessayer dans quelques instants.";

    public TalkReviewException() {
        this(REVIEW_UNAVAILABLE_MESSAGE);
    }

    public TalkReviewException(String message) {
        super(message, Response.Status.BAD_GATEWAY);
    }
}
