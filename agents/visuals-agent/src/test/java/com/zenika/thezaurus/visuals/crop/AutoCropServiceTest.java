package com.zenika.thezaurus.visuals.crop;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenika.thezaurus.visuals.crop.AutoCropService.CropResult;
import com.zenika.thezaurus.visuals.crop.AutoCropService.DetectionClient;
import com.zenika.thezaurus.visuals.crop.AutoCropService.DetectionResponse;
import com.zenika.thezaurus.visuals.crop.AutoCropService.Settings;
import com.zenika.thezaurus.visuals.crop.AutoCropService.Usage;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AutoCropServiceTest {
    private static final String DETECTION = """
            {"subject":{"x":200,"y":100,"width":600,"height":800},"face":{"x":400,"y":150,"width":200,"height":200}}
            """;
    private final Settings settings =
            new Settings(true, "gemini-3.1-flash-lite", "test-key", "", "global", "", 3, 20000);

    private AutoCropService service(DetectionClient client) {
        return new AutoCropService(settings, new ObjectMapper(), client);
    }

    private DetectionResponse response(String json) {
        return new DetectionResponse(json, new Usage(100, 40, 140));
    }

    private byte[] photo(int width, int height, int color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(color));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void nativeCoordinatesAreNormalizedAndReusedAcrossTemplates() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AutoCropService service = service((bytes, feedback) -> {
            calls.incrementAndGet();
            return response(DETECTION);
        });
        byte[] photo = photo(800, 1000, 0x123456);
        CropResult first = service.analyze(photo, 800, 1000, 300, 300, 20);
        assertEquals("accepted", first.status());
        assertEquals(.4, first.analysis().face().x());
        assertEquals(1, first.calls());
        assertEquals(new Usage(100, 40, 140), first.usage());
        CropResult other = service.analyze(photo, 800, 1000, 419, 334, 23);
        assertEquals("accepted", other.status());
        assertTrue(other.cacheHit());
        assertEquals(0, other.calls());
        assertEquals(Usage.zero(), other.usage());
        assertEquals(1, calls.get());
    }

    @Test
    void impossibleGeometryDoesNotAskGeminiAgain() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String impossible =
                "{\"subject\":{\"x\":0,\"y\":0,\"width\":1000,\"height\":1000},\"face\":{\"x\":100,\"y\":200,\"width\":800,\"height\":400}}";
        AutoCropService service = service((bytes, feedback) -> {
            calls.incrementAndGet();
            return response(impossible);
        });
        byte[] photo = photo(1600, 800, 0x123456);
        CropResult first = service.analyze(photo, 1600, 800, 200, 600, 20);
        assertEquals("fallback", first.status());
        assertEquals(1, calls.get());
        assertEquals(1, first.calls());
        CropResult again = service.analyze(photo, 1600, 800, 300, 300, 20);
        assertTrue(again.cacheHit());
        assertEquals(0, again.calls());
        assertEquals(1, calls.get());
    }

    @Test
    void onlyMalformedResponsesTriggerTwoBoundedCorrections() throws Exception {
        List<String> feedback = new ArrayList<>();
        AutoCropService service = service((bytes, correction) -> {
            feedback.add(correction);
            return response("{bad}");
        });
        CropResult result = service.analyze(photo(800, 1000, 0), 800, 1000, 300, 300, 20);
        assertEquals("fallback", result.status());
        assertEquals(3, result.calls());
        assertEquals(3, feedback.size());
        assertEquals("", feedback.getFirst());
        assertTrue(feedback.get(1).contains("0..1000"));
        assertEquals(new Usage(300, 120, 420), result.usage());
    }

    @Test
    void correctionCanRecoverFromMalformedDetection() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AutoCropService service =
                service((bytes, correction) -> response(calls.incrementAndGet() == 1 ? "{}" : DETECTION));
        CropResult result = service.analyze(photo(800, 1000, 0), 800, 1000, 300, 300, 20);
        assertEquals("accepted", result.status());
        assertEquals(2, result.calls());
        assertEquals(280, result.usage().totalTokens());
    }

    @Test
    void concurrentFramesShareDetectionAndOnlyOwnerReportsUsage() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AutoCropService service = service((bytes, correction) -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException();
            } catch (InterruptedException e) {
                throw new IllegalStateException();
            }
            return response(DETECTION);
        });
        byte[] photo = photo(800, 1000, 0);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> service.analyze(photo, 800, 1000, 300, 300, 20));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var b = executor.submit(() -> service.analyze(photo, 800, 1000, 419, 334, 23));
            release.countDown();
            CropResult first = a.get(5, TimeUnit.SECONDS), second = b.get(5, TimeUnit.SECONDS);
            assertEquals(1, calls.get());
            assertEquals(1, first.calls() + second.calls());
            assertEquals(140, first.usage().totalTokens() + second.usage().totalTokens());
            assertTrue(first.cacheHit() || second.cacheHit());
        }
    }

    @Test
    void disabledAndMissingConfigurationNeverCallProvider() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        DetectionClient client = (bytes, correction) -> {
            calls.incrementAndGet();
            return response(DETECTION);
        };
        AutoCropService disabled = new AutoCropService(
                new Settings(false, null, "", "", "global", "", 3, 20000), new ObjectMapper(), client);
        assertFalse(disabled.enabled());
        assertFalse(disabled.available());
        assertEquals(
                "disabled", disabled.analyze(photo(1, 1, 0), 1, 1, 300, 300, 20).status());
        AutoCropService missing = new AutoCropService(
                new Settings(true, null, "", "", "global", "", 3, 20000), new ObjectMapper(), client);
        CropException failure =
                assertThrows(CropException.class, () -> missing.analyze(new byte[] {1}, 1, 1, 300, 300, 20));
        assertEquals(503, failure.status());
        assertEquals(0, calls.get());
    }

    @Test
    void imageIsDownscaledToJpegBeforeAnalysis() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AutoCropService service = service((bytes, correction) -> {
            calls.incrementAndGet();
            assertEquals(255, bytes[0] & 255);
            assertEquals(216, bytes[1] & 255);
            try {
                BufferedImage scaled = ImageIO.read(new ByteArrayInputStream(bytes));
                assertEquals(1024, scaled.getHeight());
                assertEquals(683, scaled.getWidth());
            } catch (Exception e) {
                fail(e);
            }
            return response(DETECTION);
        });
        service.analyze(photo(2000, 3000, 0x123456), 2000, 3000, 300, 300, 20);
        assertEquals(1, calls.get());
    }

    @Test
    void invalidImageMetadataIsRejectedBeforeCacheOrProvider() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AutoCropService service = service((bytes, correction) -> {
            calls.incrementAndGet();
            return response(DETECTION);
        });
        byte[] photo = photo(800, 1000, 0);
        service.analyze(photo, 800, 1000, 300, 300, 20);
        CropException bad = assertThrows(CropException.class, () -> service.analyze(photo, 801, 1000, 300, 300, 20));
        assertEquals(400, bad.status());
        assertEquals(1, calls.get());
        assertThrows(CropException.class, () -> service.analyze(new byte[] {1, 2, 3}, 1, 1, 300, 300, 20));
    }

    @Test
    void providerFailuresAreNeverRetriedAsMalformedCoordinates() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AutoCropService service = service((bytes, correction) -> {
            calls.incrementAndGet();
            throw new CropException(503, "MODEL_UNAVAILABLE", "Modèle indisponible.");
        });
        assertThrows(CropException.class, () -> service.analyze(photo(800, 1000, 0), 800, 1000, 300, 300, 20));
        assertEquals(1, calls.get());
    }

    @Test
    void malformedAndInconsistentNativeBoxesAreRejected() {
        AutoCropService service = service((bytes, feedback) -> response(DETECTION));
        for (String value : List.of(
                "{}",
                "null",
                "{\"subject\":{\"x\":-1,\"y\":0,\"width\":1000,\"height\":1000},\"face\":null}",
                "{\"subject\":{\"x\":0,\"y\":0,\"width\":100,\"height\":100},\"face\":{\"x\":800,\"y\":800,\"width\":100,\"height\":100}}")) {
            assertThrows(IllegalArgumentException.class, () -> service.parseNativeAnalysis(value));
        }
    }
}
