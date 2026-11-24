package com.zenika.thezaurus.visuals.render;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Pure Java2D composition over approved blank PNG assets; no image generation. */
@ApplicationScoped
public class ImageRenderer {
    private static final BufferedImage ZENIKA_MARK = readResourceImage("branding/zenika-mark.png");
    private final TemplateCatalog catalog;
    private final Map<String, BufferedImage> backgrounds = new ConcurrentHashMap<>();
    private final Map<Integer, Font> fonts = new ConcurrentHashMap<>();

    @Inject
    public ImageRenderer(TemplateCatalog catalog) {
        this.catalog = catalog;
    }

    public RenderResult render(RenderData data) {
        TemplateSpec spec = catalog.require(data.templateId());
        if (data.people().size() > spec.photos().size())
            throw new IllegalArgumentException("Trop de personnes pour ce template.");
        boolean solo = "nc".equals(spec.id()) && data.people().size() == 1;
        String asset = solo && spec.soloBackground() != null ? spec.soloBackground() : spec.background();
        BufferedImage background = backgrounds.computeIfAbsent(asset, ImageRenderer::readResourceImage);
        if (background.getWidth() != spec.width() || background.getHeight() != spec.height()) {
            throw new IllegalStateException("Dimensions du fond incompatibles avec le catalogue : " + spec.id());
        }
        BufferedImage output = new BufferedImage(spec.width(), spec.height(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        try {
            quality(g);
            g.drawImage(background, 0, 0, null);
            g.setBackground(new Color(background.getRGB(spec.width() / 2, spec.height() / 2), true));
            if (spec.id().startsWith("blog")) renderBlog(g, spec, data);
            else if (spec.id().startsWith("conf-")) renderConference(g, spec, data);
            else renderNightClazz(g, spec, data);
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            if (!ImageIO.write(output, "png", encoded)) throw new IllegalStateException("Encodeur PNG indisponible.");
            return new RenderResult(encoded.toByteArray(), output.getWidth(), output.getHeight());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'encoder le visuel PNG.", e);
        } finally {
            g.dispose();
        }
    }

    private void renderConference(Graphics2D g, TemplateSpec spec, RenderData data) {
        for (int i = 0; i < spec.photos().size(); i++) {
            RenderData.Person person = person(data, i);
            drawPhoto(g, spec.photos().get(i), person.photo(), person.crop(), false);
            text(g, person.name(), spec.typography().names().get(i), 1);
        }
        for (int i = 0; i < spec.typography().titles().size(); i++) {
            text(
                    g,
                    i == 0 ? data.title() : data.title2(),
                    spec.typography().titles().get(i),
                    2);
        }
        if (spec.logo() != null) {
            if (data.logo() != null) drawPhoto(g, spec.logo(), data.logo(), RenderData.Crop.centered(), true);
            else {
                List<Double> b = spec.logo().box();
                text(
                        g,
                        data.conference(),
                        new TemplateSpec.TextStyle(
                                List.of(b.get(0) + 18, b.get(1) + 18, b.get(2) - 36, b.get(3) - 36),
                                44,
                                700,
                                "center",
                                "#ffffff",
                                null,
                                1.17,
                                "center"),
                        2);
            }
        }
    }

    private void renderBlog(Graphics2D g, TemplateSpec spec, RenderData data) {
        text(g, data.title(), spec.typography().title(), 2);
        if ("blog".equals(spec.id())) {
            RenderData.Person person = person(data, 0);
            String[] name = clean(person.name()).split("\\s+", 2);
            text(g, name[0], spec.typography().names().get(0), 2);
            text(g, name.length > 1 ? name[1] : "", spec.typography().names().get(1), 2);
            drawPhoto(g, spec.photos().getFirst(), person.photo(), person.crop(), false);
        } else {
            for (int i = 0; i < spec.photos().size(); i++) {
                RenderData.Person person = person(data, i);
                text(g, person.name(), spec.typography().names().get(i), 2);
                drawPhoto(g, spec.photos().get(i), person.photo(), person.crop(), false);
            }
        }
    }

    private void renderNightClazz(Graphics2D g, TemplateSpec spec, RenderData data) {
        text(g, data.title(), spec.typography().title(), 2);
        badges(g, spec, List.of(clean(data.date()), clean(data.hour()), clean(data.location())));
        int count = "nc".equals(spec.id()) && data.people().size() == 1
                ? 1
                : spec.photos().size();
        for (int i = 0; i < count; i++) {
            RenderData.Person person = person(data, i);
            drawPhoto(g, spec.photos().get(i), person.photo(), person.crop(), false);
            text(g, person.name(), spec.typography().names().get(i), 2);
        }
        if (spec.typography().partner() != null)
            text(g, data.partner(), spec.typography().partner(), 1);
        if (spec.logo() != null && data.logo() != null) {
            drawPhoto(g, spec.logo(), data.logo(), RenderData.Crop.centered(), true);
        }
    }

    private static RenderData.Person person(RenderData data, int index) {
        return index < data.people().size() ? data.people().get(index) : new RenderData.Person("", null, null);
    }

    private static void drawPhoto(
            Graphics2D original, TemplateSpec.ImageSlot slot, byte[] bytes, RenderData.Crop crop, boolean contain) {
        List<Double> b = slot.box();
        double x = b.get(0), y = b.get(1), w = b.get(2), h = b.get(3);
        // Java2D's geometric clip is not antialiased. Compose a transparent
        // photo layer with an antialiased alpha mask instead, avoiding jagged
        // corners or a backing-color fringe around the approved photograph.
        int layerWidth = (int) Math.ceil(w) + 2, layerHeight = (int) Math.ceil(h) + 2;
        BufferedImage layer = new BufferedImage(layerWidth, layerHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = layer.createGraphics();
        try {
            quality(g);
            g.setBackground(original.getBackground());
            g.translate(1 - x, 1 - y);
            if (bytes != null) {
                BufferedImage photo = decodeImage(bytes);
                double scale = (contain
                                ? Math.min(w / photo.getWidth(), h / photo.getHeight())
                                : Math.max(w / photo.getWidth(), h / photo.getHeight()))
                        * crop.zoom();
                double width = photo.getWidth() * scale, height = photo.getHeight() * scale;
                double px = x + (w - width) / 2 + crop.x() * Math.max(0, (width - w) / 2);
                double py = y + (h - height) / 2 + crop.y() * Math.max(0, (height - h) / 2);
                AffineTransform placement = AffineTransform.getTranslateInstance(px, py);
                placement.scale(scale, scale);
                g.drawImage(photo, placement, null);
            } else {
                double size = Math.min(w, h) * .70;
                AffineTransform placement =
                        AffineTransform.getTranslateInstance(x + (w - size) / 2, y + (h - size) / 2);
                placement.scale(size / ZENIKA_MARK.getWidth(), size / ZENIKA_MARK.getHeight());
                g.drawImage(ZENIKA_MARK, placement, null);
            }
            BufferedImage mask = new BufferedImage(layerWidth, layerHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D mg = mask.createGraphics();
            try {
                quality(mg);
                mg.setColor(Color.WHITE);
                mg.fill(new RoundRectangle2D.Double(1, 1, w, h, slot.radius() * 2, slot.radius() * 2));
            } finally {
                mg.dispose();
            }
            g.setTransform(new AffineTransform());
            g.setComposite(AlphaComposite.DstIn);
            g.drawImage(mask, 0, 0, null);
        } finally {
            g.dispose();
        }
        original.drawImage(layer, AffineTransform.getTranslateInstance(x - 1, y - 1), null);
    }

    private void text(Graphics2D original, String value, TemplateSpec.TextStyle style, int maxLines) {
        if (style == null || clean(value).isEmpty()) return;
        String content = clean(value);
        List<Double> b = style.box();
        double size = style.size();
        List<String> rows;
        Font font;
        while (true) {
            font = font(style.weight(), size);
            rows = wrap(content, font, original, b.get(2));
            if (rows.size() <= maxLines
                    && rows.size() * size * style.lineHeight() <= b.get(3)
                    && rowsFit(rows, font, original, b.get(2))) break;
            if (size <= 1) throw new IllegalArgumentException("Le texte ne tient pas dans la zone du template.");
            size = Math.max(1, size - 1);
        }
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.clip(new Rectangle2D.Double(b.get(0), b.get(1), b.get(2), b.get(3)));
            g.setFont(font);
            g.setColor(Color.decode(style.color()));
            double first;
            if (style.baseline() != null) {
                first = style.baseline()
                        - ("down".equals(style.multiline()) ? 0 : (rows.size() - 1) * size * style.lineHeight() / 2);
            } else {
                TextLayout sample = new TextLayout("Ag", font, g.getFontRenderContext());
                first = b.get(1)
                        + b.get(3) / 2
                        + (sample.getAscent() - sample.getDescent()) / 2
                        - (rows.size() - 1) * size * style.lineHeight() / 2;
            }
            for (int i = 0; i < rows.size(); i++) {
                TextLayout layout = new TextLayout(rows.get(i), font, g.getFontRenderContext());
                double x = b.get(0)
                        + ("center".equals(style.align())
                                ? (b.get(2) - layout.getAdvance()) / 2
                                : "right".equals(style.align()) ? b.get(2) - layout.getAdvance() : 0);
                layout.draw(g, (float) x, (float) (first + i * size * style.lineHeight()));
            }
        } finally {
            g.dispose();
        }
    }

    private Font font(int weight, double size) {
        Font base = fonts.computeIfAbsent(weight, w -> {
            try (InputStream input = ImageRenderer.class.getResourceAsStream("/fonts/nunito-" + w + ".ttf")) {
                if (input == null) throw new IllegalStateException("Police embarquée introuvable : Nunito " + w);
                return Font.createFont(Font.TRUETYPE_FONT, input)
                        .deriveFont(Map.of(
                                TextAttribute.KERNING, TextAttribute.KERNING_ON,
                                TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON));
            } catch (Exception e) {
                throw new IllegalStateException("Impossible de charger la police Nunito " + w, e);
            }
        });
        return base.deriveFont((float) size);
    }

    private static List<String> wrap(String content, Font font, Graphics2D g, double width) {
        List<String> rows = new ArrayList<>();
        String current = "";
        for (String word : content.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && advance(candidate, font, g) > width) {
                rows.add(current);
                current = word;
            } else current = candidate;
        }
        if (!current.isEmpty()) rows.add(current);
        return rows;
    }

    private static double advance(String content, Font font, Graphics2D g) {
        return new TextLayout(content, font, g.getFontRenderContext()).getAdvance();
    }

    private static boolean rowsFit(List<String> rows, Font font, Graphics2D g, double width) {
        for (String row : rows) if (advance(row, font, g) > width) return false;
        return true;
    }

    private void badges(Graphics2D original, TemplateSpec spec, List<String> values) {
        TemplateSpec.Badges style = spec.badges();
        if (style == null) return;
        Font font = font(style.fontWeight(), style.fontSize());
        double[] widths = values.stream()
                .mapToDouble(value -> Math.min(
                        style.maxWidth(),
                        Math.max(
                                style.minWidth(),
                                (value.isEmpty() ? 0 : advance(value, font, original)) + style.padding())))
                .toArray();
        double total = widths[0] + widths[1] + widths[2] + style.gap() * 2;
        double x = (spec.width() - total) / 2;
        for (int i = 0; i < values.size(); i++) {
            Graphics2D g = (Graphics2D) original.create();
            try {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(
                        x, style.y(), widths[i], style.height(), style.height(), style.height()));
                badgeIcon(g, i, x + 24, style.y() + style.height() / 2, Color.decode(style.color()));
            } finally {
                g.dispose();
            }
            text(
                    original,
                    values.get(i),
                    new TemplateSpec.TextStyle(
                            List.of(x + 40, style.y() + 4, widths[i] - 49, style.height() - 8),
                            style.fontSize(),
                            style.fontWeight(),
                            "center",
                            style.color(),
                            null,
                            1.17,
                            "center"),
                    1);
            x += widths[i] + style.gap();
        }
    }

    private static void badgeIcon(Graphics2D g, int index, double x, double y, Color color) {
        g.translate(x, y);
        g.setColor(color);
        g.setStroke(new BasicStroke(2));
        if (index == 0) {
            g.draw(new Rectangle2D.Double(-7, -7, 14, 14));
            g.fill(new Rectangle2D.Double(-7, -5, 14, 4));
            g.fill(new Rectangle2D.Double(-4, -11, 2, 6));
            g.fill(new Rectangle2D.Double(3, -11, 2, 6));
        } else if (index == 1) {
            g.draw(new Ellipse2D.Double(-8, -8, 16, 16));
            g.draw(new Line2D.Double(0, -5, 0, 0));
            g.draw(new Line2D.Double(0, 0, 4, 0));
        } else {
            Path2D marker = new Path2D.Double();
            marker.moveTo(-7, -2);
            marker.curveTo(-7, -11, 7, -11, 7, -2);
            marker.lineTo(0, 10);
            marker.closePath();
            g.fill(marker);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(-2, -4, 4, 4));
        }
    }

    private static BufferedImage decodeImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new IllegalArgumentException("Image illisible ou format non pris en charge.");
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("Image illisible.", e);
        }
    }

    private static BufferedImage readResourceImage(String path) {
        try (InputStream input = ImageRenderer.class.getResourceAsStream("/" + path)) {
            if (input == null) throw new IllegalStateException("Fond de template introuvable : " + path);
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IllegalStateException("Fond de template invalide : " + path);
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fond du template.", e);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
