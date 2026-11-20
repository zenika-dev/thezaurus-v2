package com.zenika.thezaurus.visuals.render;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Native, shared image and typography coordinates for one approved template. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateSpec(
        String id,
        String label,
        String category,
        int width,
        int height,
        String background,
        String soloBackground,
        List<ImageSlot> photos,
        ImageSlot logo,
        Typography typography,
        Badges badges) {
    public TemplateSpec {
        if (id == null || id.isBlank() || width < 1 || height < 1 || background == null || typography == null) {
            throw new IllegalArgumentException("Définition de template invalide.");
        }
        photos = photos == null ? List.of() : List.copyOf(photos);
        if (badges == null && ("nc".equals(id) || "nc-partenaire".equals(id))) {
            badges = new Badges(237, 41, 15, 285, 90, 52, 25, 700, "#eb1552");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageSlot(List<Double> box, double radius) {
        public ImageSlot {
            box = checkedBox(box);
            if (!Double.isFinite(radius) || radius < 0 || radius > Math.min(box.get(2), box.get(3)) / 2) {
                throw new IllegalArgumentException("Arrondi d'image invalide.");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Typography(List<TextStyle> names, List<TextStyle> titles, TextStyle title, TextStyle partner) {
        public Typography {
            names = names == null ? List.of() : List.copyOf(names);
            titles = titles == null ? (title == null ? List.of() : List.of(title)) : List.copyOf(titles);
            if (title == null && !titles.isEmpty()) title = titles.getFirst();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextStyle(
            List<Double> box,
            double size,
            int weight,
            String align,
            String color,
            Double baseline,
            double lineHeight,
            String multiline) {
        public TextStyle {
            box = checkedBox(box);
            if (!Double.isFinite(size) || size <= 0) throw new IllegalArgumentException("Taille de police invalide.");
            if (baseline != null && !Double.isFinite(baseline))
                throw new IllegalArgumentException("Baseline invalide.");
            if (lineHeight == 0) lineHeight = 1.17;
            if (!Double.isFinite(lineHeight) || lineHeight <= 0)
                throw new IllegalArgumentException("Interligne invalide.");
            if (align == null) align = "center";
            if (!List.of("left", "center", "right").contains(align))
                throw new IllegalArgumentException("Alignement invalide.");
            if (color == null || !color.matches("#[a-fA-F0-9]{6}"))
                throw new IllegalArgumentException("Couleur invalide.");
            if (multiline == null) multiline = "center";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Badges(
            double y,
            double height,
            double gap,
            double maxWidth,
            double minWidth,
            double padding,
            double fontSize,
            int fontWeight,
            String color) {}

    private static List<Double> checkedBox(List<Double> value) {
        if (value == null
                || value.size() != 4
                || value.stream().anyMatch(n -> n == null || !Double.isFinite(n))
                || value.get(2) <= 0
                || value.get(3) <= 0) {
            throw new IllegalArgumentException("Zone de template invalide.");
        }
        return List.copyOf(value);
    }
}
