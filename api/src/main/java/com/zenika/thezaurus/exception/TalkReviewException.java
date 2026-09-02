package com.zenika.thezaurus.exception;

import jakarta.ws.rs.core.Response;

public class TalkReviewException extends ThezaurusException {
    public TalkReviewException(String message) {
        super(message, Response.Status.BAD_GATEWAY);
    }
}
