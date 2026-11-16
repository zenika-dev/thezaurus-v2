package com.zenika.thezaurus.visuals.crop;

/** Safe public failure; never attaches a provider body, credential or photo. */
public final class CropException extends RuntimeException {
    private final int status;
    private final String code;

    public CropException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
