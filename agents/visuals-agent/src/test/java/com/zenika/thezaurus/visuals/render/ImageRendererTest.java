package com.zenika.thezaurus.visuals.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageRendererTest {
    private final TemplateCatalog catalog = new TemplateCatalog();
    private final ImageRenderer renderer = new ImageRenderer(catalog);

    @Test
    void missingPhotosDisplayZenikaLogoInEachSlot() throws IOException {
        for (String id : List.of("conf-1", "conf-1-binome", "conf-1-trinome", "blog", "blog-2")) {
            var spec = catalog.require(id);
            var people = spec.photos().stream()
                    .map(slot -> new RenderData.Person("Sans accord", null, null))
                    .toList();
            var image = decode(renderer.render(data(id, people, null)).png());
            for (var slot : spec.photos()) {
                var box = slot.box();
                double size = Math.min(box.get(2), box.get(3)) * .70;
                int x = (int) (box.get(0) + box.get(2) / 2);
                int y = (int) (box.get(1) + (box.get(3) - size) / 2 + size * .12);
                var color = new Color(image.getRGB(x, y), true);
                assertTrue(color.getRed() > 200 && color.getGreen() < 80, "Zenika red logo: " + id);
            }
        }
    }

    @Test
    void loadsEightImmutableNativeTemplatesAndRendersStandalonePngs() throws IOException {
        assertEquals(8, catalog.list().size());
        assertThrows(UnsupportedOperationException.class, () -> catalog.list().clear());
        assertThrows(IllegalArgumentException.class, () -> catalog.require("unknown"));
        for (TemplateSpec spec : catalog.list()) {
            List<RenderData.Person> people = new ArrayList<>();
            for (int i = 0; i < spec.photos().size(); i++)
                people.add(new RenderData.Person("Élodie Martin " + i, null, null));
            RenderResult result = renderer.render(data(spec.id(), people, null));
            BufferedImage actual = decode(result.png());
            assertEquals(1080, result.width(), spec.id());
            assertEquals(spec.id().startsWith("blog") ? 1350 : 1080, result.height(), spec.id());
            assertEquals(spec.width(), actual.getWidth());
            assertEquals(spec.height(), actual.getHeight());
            assertArrayEquals(
                    new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, java.util.Arrays.copyOf(result.png(), 8));
            BufferedImage background = background(spec.background());
            assertEquals(
                    background.getRGB(20, 20),
                    actual.getRGB(20, 20),
                    "Fixed branding must remain unchanged: " + spec.id());
        }
    }

    @Test
    void usesTheCalculatedCropAndPreservesPixelsOutsideTheSlot() throws IOException {
        BufferedImage source = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        try {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 300, 300);
            g.setColor(Color.BLUE);
            g.fillRect(300, 0, 300, 300);
        } finally {
            g.dispose();
        }
        RenderData input = data(
                "conf-1", List.of(new RenderData.Person("Alice", png(source), new RenderData.Crop(1, -1, 0))), null);
        BufferedImage actual = decode(renderer.render(input).png());
        assertEquals(Color.BLUE.getRGB(), actual.getRGB(230, 600));
        assertEquals(background(catalog.require("conf-1").background()).getRGB(65, 450), actual.getRGB(65, 450));
        assertArrayEquals(
                renderer.render(input).png(), renderer.render(input).png(), "Repeated composition is deterministic");
    }

    @Test
    void keepsTheCardBackgroundBehindTransparentPhotosAndAtRoundedCorners() throws IOException {
        BufferedImage transparent = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        byte[] photo = png(transparent);
        TemplateSpec spec = catalog.require("conf-1-binome");
        BufferedImage actual = decode(renderer.render(data(
                        spec.id(),
                        List.of(
                                new RenderData.Person("Alice", photo, null),
                                new RenderData.Person("Thomas", photo, null)),
                        null))
                .png());
        BufferedImage background = background(spec.background());
        for (TemplateSpec.ImageSlot slot : spec.photos()) {
            List<Double> b = slot.box();
            int x = b.get(0).intValue(),
                    y = b.get(1).intValue(),
                    w = b.get(2).intValue(),
                    h = b.get(3).intValue();
            for (int[] point : List.of(
                    new int[] {x + 1, y + 1}, new int[] {x + 16, y + 16}, new int[] {x + w / 2, y + h / 2}, new int[] {
                        x + 1, y + h - 2
                    })) {
                assertEquals(background.getRGB(point[0], point[1]), actual.getRGB(point[0], point[1]));
                Color color = new Color(actual.getRGB(point[0], point[1]), true);
                assertFalse(
                        color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200,
                        "No white placeholder remains behind a transparent photograph");
            }
        }
    }

    @Test
    void drawsAMissingPhotoLogoWithoutAFlatBackgroundRectangle() throws IOException {
        TemplateSpec spec = catalog.require("blog");
        BufferedImage actual =
                decode(renderer.render(data(spec.id(), List.of(new RenderData.Person("Alice", null, null)), null))
                        .png());
        BufferedImage background = background(spec.background());
        List<Double> b = spec.photos().getFirst().box();
        int x = (int) (b.get(0) + b.get(2) / 2), y = (int) (b.get(1) + b.get(3) * .05);
        assertEquals(background.getRGB(x, y), actual.getRGB(x, y), "Empty space above logo keeps native background");
        double size = Math.min(b.get(2), b.get(3)) * .70;
        int logoY = (int) (b.get(1) + (b.get(3) - size) / 2 + size * .12);
        assertTrue(actual.getRGB(x, logoY) != background.getRGB(x, logoY));
    }

    @Test
    void containsLogosWithoutCroppingOrAddingAWhiteBacking() throws IOException {
        BufferedImage source = new BufferedImage(600, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        try {
            g.setColor(Color.RED);
            g.fillRect(0, 0, 600, 100);
        } finally {
            g.dispose();
        }
        TemplateSpec spec = catalog.require("conf-1");
        BufferedImage actual = decode(
                renderer.render(data(spec.id(), List.of(new RenderData.Person("Alice", null, null)), png(source)))
                        .png());
        BufferedImage background = background(spec.background());
        List<Double> b = spec.logo().box();
        int cx = (int) (b.get(0) + b.get(2) / 2), cy = (int) (b.get(1) + b.get(3) / 2);
        assertEquals(Color.RED.getRGB(), actual.getRGB(cx, cy));
        assertEquals(
                background.getRGB(cx, b.get(1).intValue() + 5),
                actual.getRGB(cx, b.get(1).intValue() + 5));
    }

    @Test
    void usesTheSoloNightClazzAssetWithoutDrawingASecondSilhouette() throws IOException {
        TemplateSpec spec = catalog.require("nc");
        assertNotNull(spec.soloBackground());
        BufferedImage actual =
                decode(renderer.render(data("nc", List.of(new RenderData.Person("Alice", null, null)), null))
                        .png());
        BufferedImage solo = background(spec.soloBackground());
        List<Double> second = spec.photos().get(1).box();
        int x = (int) (second.get(0) + second.get(2) / 2), y = (int) (second.get(1) + second.get(3) / 2);
        assertEquals(solo.getRGB(x, y), actual.getRGB(x, y));
    }

    @Test
    void rejectsInvalidInternalCropsAndKeepsByteBuffersImmutable() {
        assertThrows(IllegalArgumentException.class, () -> new RenderData.Crop(Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new RenderData.Crop(1, 2, 0));
        byte[] source = {1, 2};
        RenderData.Person person = new RenderData.Person("A", source, null);
        source[0] = 7;
        assertEquals(1, person.photo()[0]);
        byte[] returned = person.photo();
        returned[0] = 9;
        assertEquals(1, person.photo()[0]);
    }

    private static RenderData data(String id, List<RenderData.Person> people, byte[] logo) {
        return new RenderData(
                id,
                "Les agents IA avec Quarkus",
                "Java en production",
                "Devoxx France",
                "19/11",
                "18h30–21h",
                "Zenika Nantes",
                "Google Cloud",
                people,
                logo);
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static BufferedImage background(String resource) throws IOException {
        try (InputStream input = ImageRendererTest.class.getResourceAsStream("/" + resource)) {
            assertNotNull(input, resource);
            return ImageIO.read(input);
        }
    }
}
