package com.zenika.thezaurus.visuals.render;

import java.util.List;

/** Validated visual inputs; crop is an internal result, not an end-user API field. */
public record RenderData(
        String templateId,
        String title,
        String title2,
        String conference,
        String date,
        String hour,
        String location,
        String partner,
        List<Person> people,
        byte[] logo) {
    public RenderData {
        people = people == null ? List.of() : List.copyOf(people);
        logo = logo == null ? null : logo.clone();
    }

    @Override
    public byte[] logo() {
        return logo == null ? null : logo.clone();
    }

    public record Person(String name, byte[] photo, Crop crop) {
        public Person {
            photo = photo == null ? null : photo.clone();
            if (crop == null) crop = Crop.centered();
        }

        @Override
        public byte[] photo() {
            return photo == null ? null : photo.clone();
        }
    }

    /** Zoom relative to cover; x/y are normalized offsets along the available overflow. */
    public record Crop(double zoom, double x, double y) {
        public Crop {
            if (!Double.isFinite(zoom)
                    || zoom < 1
                    || zoom > 4
                    || !Double.isFinite(x)
                    || x < -1
                    || x > 1
                    || !Double.isFinite(y)
                    || y < -1
                    || y > 1) {
                throw new IllegalArgumentException("Cadrage calculé invalide.");
            }
        }

        public static Crop centered() {
            return new Crop(1, 0, 0);
        }

        /** Largest cover rectangle, centered horizontally and aligned with the source's top edge. */
        public static Crop topCentered() {
            return new Crop(1, 0, 1);
        }
    }
}
