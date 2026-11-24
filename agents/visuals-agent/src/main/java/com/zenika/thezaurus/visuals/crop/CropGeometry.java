package com.zenika.thezaurus.visuals.crop;

import java.util.List;

/** Pure geometry; AI identifies regions but never draws output pixels. */
public final class CropGeometry {
    private static final double EPS = 1e-8;

    private CropGeometry() {}

    public record Box(double x, double y, double width, double height) {
        public Box {
            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(width)
                    || !Double.isFinite(height)
                    || x < 0
                    || y < 0
                    || width <= 0
                    || height <= 0
                    || x + width > 1 + EPS
                    || y + height > 1 + EPS) {
                throw new IllegalArgumentException("Rectangle hors de l'image ou dimensions invalides.");
            }
        }
    }

    public record Analysis(Box subject, Box face) {
        public Analysis {
            if (subject == null) throw new IllegalArgumentException("Sujet requis.");
            if (face != null
                    && (face.x < subject.x - .02
                            || face.y < subject.y - .02
                            || face.x + face.width > subject.x + subject.width + .02
                            || face.y + face.height > subject.y + subject.height + .02)) {
                throw new IllegalArgumentException("Le visage doit appartenir au sujet identifié.");
            }
        }
    }

    public record Crop(double zoom, double x, double y) {
        public static Crop centered() {
            return new Crop(1, 0, 0);
        }
    }

    public record Plan(Crop crop, String status, List<String> issues, Box viewport) {
        public Plan {
            issues = List.copyOf(issues);
        }
    }

    private record Rect(double x, double y, double width, double height) {}

    private record Choice(Rect view, double zoom) {}

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    private static Rect pixels(Box box, double width, double height) {
        return new Rect(box.x * width, box.y * height, box.width * width, box.height * height);
    }

    private static Rect union(Rect a, Rect b) {
        double x = Math.min(a.x, b.x), y = Math.min(a.y, b.y);
        return new Rect(x, y, Math.max(a.x + a.width, b.x + b.width) - x, Math.max(a.y + a.height, b.y + b.height) - y);
    }

    private static Rect viewport(double cx, double cy, double width, double height, double iw, double ih) {
        return new Rect(clamp(cx - width / 2, 0, iw - width), clamp(cy - height / 2, 0, ih - height), width, height);
    }

    private static boolean fits(Rect required, Rect view, double fw, double fh, double radius) {
        double r = Math.min(radius, Math.min(fw, fh) / 2);
        for (double x : new double[] {required.x, required.x + required.width}) {
            for (double y : new double[] {required.y, required.y + required.height}) {
                double px = (x - view.x) / view.width * fw, py = (y - view.y) / view.height * fh;
                if (px < -EPS || py < -EPS || px > fw + EPS || py > fh + EPS) return false;
                double dx = Math.max(Math.max(r - px, 0), px - (fw - r));
                double dy = Math.max(Math.max(r - py, 0), py - (fh - r));
                if (r > 0 && dx * dx + dy * dy > r * r + EPS) return false;
            }
        }
        return true;
    }

    public static Plan plan(
            Analysis analysis, int imageWidth, int imageHeight, int frameWidth, int frameHeight, int radius) {
        if (analysis == null
                || imageWidth <= 0
                || imageHeight <= 0
                || frameWidth <= 0
                || frameHeight <= 0
                || radius < 0) {
            throw new IllegalArgumentException("Dimensions de cadrage invalides.");
        }
        double iw = imageWidth, ih = imageHeight, fw = frameWidth, fh = frameHeight;
        Rect subject = pixels(analysis.subject, iw, ih);
        Rect face = analysis.face == null ? null : pixels(analysis.face, iw, ih);
        Rect required = subject;
        if (face != null) {
            // Headroom is a preference, not invented pixels. Clamp it to the
            // existing photo instead of asking the model to shrink the face.
            double x = Math.max(0, face.x - face.width * .08);
            double y = Math.max(0, face.y - face.height * .08);
            required = new Rect(
                    x, y, Math.min(iw, face.x + face.width * 1.08) - x, Math.min(ih, face.y + face.height * 1.08) - y);
        }
        Rect target = union(subject, required);
        double cover = Math.max(fw / iw, fh / ih), maxWidth = fw / cover, maxHeight = fh / cover;
        double preferredZoom = clamp(Math.min(maxWidth / target.width, maxHeight / target.height), 1, 4);
        double preferredX = target.x + target.width / 2, preferredY = target.y + target.height / 2;
        Choice chosen = null;
        for (int step = 0; step <= 96; step++) {
            double zoom = preferredZoom - (preferredZoom - 1) * step / 96;
            double width = maxWidth / zoom, height = maxHeight / zoom;
            Rect preferred = viewport(preferredX, preferredY, width, height, iw, ih);
            Rect centered =
                    viewport(required.x + required.width / 2, required.y + required.height / 2, width, height, iw, ih);
            if (fits(required, preferred, fw, fh, radius)) {
                chosen = new Choice(preferred, zoom);
                break;
            }
            if (fits(required, centered, fw, fh, radius)) {
                double low = 0, high = 1;
                for (int i = 0; i < 32; i++) {
                    double t = (low + high) / 2;
                    Rect candidate = new Rect(
                            centered.x + (preferred.x - centered.x) * t,
                            centered.y + (preferred.y - centered.y) * t,
                            width,
                            height);
                    if (fits(required, candidate, fw, fh, radius)) low = t;
                    else high = t;
                }
                chosen = new Choice(
                        new Rect(
                                centered.x + (preferred.x - centered.x) * low,
                                centered.y + (preferred.y - centered.y) * low,
                                width,
                                height),
                        zoom);
                break;
            }
        }
        List<String> issues = List.of();
        if (chosen == null) {
            chosen = new Choice(
                    viewport(
                            required.x + required.width / 2,
                            required.y + required.height / 2,
                            maxWidth,
                            maxHeight,
                            iw,
                            ih),
                    1);
            issues = List.of(
                    face != null
                            ? "Le visage entier ne tient pas dans le cadre arrondi."
                            : "Le sujet entier ne tient pas dans les proportions du cadre.");
        }
        Rect view = chosen.view;
        double scale = cover * chosen.zoom;
        double limitX = Math.max(0, (iw * scale - fw) / 2), limitY = Math.max(0, (ih * scale - fh) / 2);
        Crop crop = new Crop(
                chosen.zoom,
                limitX > EPS ? clamp(((iw - view.width) / 2 - view.x) * scale / limitX, -1, 1) : 0,
                limitY > EPS ? clamp(((ih - view.height) / 2 - view.y) * scale / limitY, -1, 1) : 0);
        return new Plan(
                crop,
                issues.isEmpty() ? "accepted" : "fallback",
                issues,
                new Box(view.x / iw, view.y / ih, view.width / iw, view.height / ih));
    }
}
