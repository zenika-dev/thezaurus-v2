package com.zenika.thezaurus.visuals.client;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/** Bounded image loading without remote fetching. */
@ApplicationScoped
public class ImageInputClient {
    private static final int LIMIT = 10 * 1024 * 1024;

    public record Image(byte[] bytes, int width, int height) {}

    public Image load(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            byte[] bytes;
            if (value.startsWith("https://") || value.startsWith("http://")) {
                throw new IllegalArgumentException(
                        "Les URL de photos ne sont pas prises en charge. Fournir une image base64.");
            } else {
                if (value.startsWith("data:") && !value.matches("(?s)^data:image/(?:png|jpeg);base64,.+$"))
                    throw invalid();
                String data = value.startsWith("data:") ? value.substring(value.indexOf(',') + 1) : value;
                if (data.length() > (LIMIT * 4L / 3 + 16)) throw invalid();
                bytes = Base64.getDecoder().decode(data);
            }
            if (bytes.length == 0 || bytes.length > LIMIT) throw invalid();
            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw invalid();
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1
                            || height < 1
                            || width > 12000
                            || height > 12000
                            || (long) width * height > 40_000_000) throw invalid();
                    if (!java.util.Set.of("JPEG", "JPG", "PNG")
                            .contains(reader.getFormatName().toUpperCase(java.util.Locale.ROOT))) throw invalid();
                    return new Image(bytes, width, height);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw invalid();
        }
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException(
                "Image invalide : utiliser un PNG/JPEG base64 (10 Mo, 40 mégapixels maximum).");
    }
}
