package com.zenika.thezaurus.visuals.render;

public record RenderResult(byte[] png, int width, int height) {
    public RenderResult {
        png = png.clone();
    }

    @Override
    public byte[] png() {
        return png.clone();
    }
}
