package com.zenika.thezaurus.visuals.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PhotoInput(String url, String base64) {

    public static PhotoInput ofUrl(String url) {
        return new PhotoInput(url, null);
    }

    public static PhotoInput ofBase64(String base64) {
        return new PhotoInput(null, base64);
    }
}
