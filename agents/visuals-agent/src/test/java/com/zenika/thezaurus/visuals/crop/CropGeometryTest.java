package com.zenika.thezaurus.visuals.crop;

import static org.junit.jupiter.api.Assertions.*;

import com.zenika.thezaurus.visuals.crop.CropGeometry.Analysis;
import com.zenika.thezaurus.visuals.crop.CropGeometry.Box;
import com.zenika.thezaurus.visuals.crop.CropGeometry.Plan;
import org.junit.jupiter.api.Test;

class CropGeometryTest {
    private final Analysis portrait = new Analysis(new Box(.2, .1, .6, .8), new Box(.4, .15, .2, .2));

    private void covers(Plan plan, int iw, int ih, int fw, int fh) {
        double scale =
                Math.max((double) fw / iw, (double) fh / ih) * plan.crop().zoom();
        double x = (fw - iw * scale) / 2 + plan.crop().x() * Math.max(0, (iw * scale - fw) / 2);
        double y = (fh - ih * scale) / 2 + plan.crop().y() * Math.max(0, (ih * scale - fh) / 2);
        assertTrue(x <= 1e-6 && y <= 1e-6 && x + iw * scale >= fw - 1e-6 && y + ih * scale >= fh - 1e-6);
        assertEquals(-x / scale / iw, plan.viewport().x(), 1e-6);
        assertEquals(-y / scale / ih, plan.viewport().y(), 1e-6);
        assertTrue(plan.crop().zoom() >= 1 && plan.crop().zoom() <= 4);
    }

    @Test
    void portraitPreservesFaceAndCoversFrame() {
        Plan plan = CropGeometry.plan(portrait, 1200, 1600, 300, 300, 25);
        assertEquals("accepted", plan.status());
        assertTrue(plan.issues().isEmpty());
        covers(plan, 1200, 1600, 300, 300);
        assertTrue(plan.viewport().y() <= .15 - .2 * .08 + 1e-6);
    }

    @Test
    void landscapeFaceOnRightMovesPhotoLeft() {
        Analysis analysis = new Analysis(new Box(.65, .12, .25, .75), new Box(.72, .18, .1, .2));
        Plan plan = CropGeometry.plan(analysis, 2400, 1200, 300, 400, 25);
        assertEquals("accepted", plan.status());
        assertTrue(plan.crop().x() < 0);
        covers(plan, 2400, 1200, 300, 400);
    }

    @Test
    void roundedCornersCanRequireWiderCrop() {
        Analysis analysis = new Analysis(new Box(.3, .3, .4, .4), new Box(.3, .3, .4, .4));
        Plan plain = CropGeometry.plan(analysis, 1000, 1000, 300, 300, 0);
        Plan round = CropGeometry.plan(analysis, 1000, 1000, 300, 300, 150);
        assertEquals("accepted", round.status());
        assertTrue(round.crop().zoom() < plain.crop().zoom());
        covers(round, 1000, 1000, 300, 300);
    }

    @Test
    void headroomIsClampedToSourceInsteadOfInvented() {
        Analysis analysis = new Analysis(new Box(.1, 0, .8, .9), new Box(.3, 0, .3, .2));
        Plan plan = CropGeometry.plan(analysis, 1200, 1600, 300, 300, 25);
        assertEquals("accepted", plan.status());
        assertTrue(plan.issues().isEmpty());
        assertEquals(0, plan.viewport().y(), 1e-8);
        covers(plan, 1200, 1600, 300, 300);
    }

    @Test
    void impossibleFaceIsReportedWithoutEmptyFrame() {
        Analysis analysis = new Analysis(new Box(0, 0, 1, 1), new Box(.1, .2, .8, .4));
        Plan plan = CropGeometry.plan(analysis, 1600, 800, 200, 600, 20);
        assertEquals("fallback", plan.status());
        assertFalse(plan.issues().isEmpty());
        covers(plan, 1600, 800, 200, 600);
    }

    @Test
    void objectWithoutFaceMustRemainWhole() {
        Plan good = CropGeometry.plan(new Analysis(new Box(.3, .3, .3, .3), null), 1200, 1600, 300, 300, 25);
        assertEquals("accepted", good.status());
        Plan bad = CropGeometry.plan(new Analysis(new Box(0, 0, 1, 1), null), 1200, 1600, 300, 300, 25);
        assertEquals("fallback", bad.status());
        covers(bad, 1200, 1600, 300, 300);
    }

    @Test
    void malformedBoxesAndDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Box(-.1, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Box(0, Double.NaN, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 0, 1.01, 1));
        assertThrows(
                IllegalArgumentException.class, () -> new Analysis(new Box(.1, .1, .2, .2), new Box(.7, .7, .1, .1)));
        assertThrows(IllegalArgumentException.class, () -> CropGeometry.plan(portrait, 0, 100, 300, 300, 25));
    }
}
