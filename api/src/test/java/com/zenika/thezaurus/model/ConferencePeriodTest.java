package com.zenika.thezaurus.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class ConferencePeriodTest {

    @Test
    public void singleDayBuildsADayPrecisionPeriod() {
        ConferencePeriod period = ConferencePeriod.singleDay("2026-03-12");

        assertEquals("2026-03-12", period.getStart());
        assertEquals("2026-03-12", period.getEnd());
        assertEquals(DatePrecision.DAY, period.getPrecision());
    }

    @Test
    public void singleDayReturnsNullForNullBlankOrInvalidInput() {
        assertNull(ConferencePeriod.singleDay(null));
        assertNull(ConferencePeriod.singleDay(""));
        assertNull(ConferencePeriod.singleDay("   "));
        assertNull(ConferencePeriod.singleDay("not-a-date"));
    }

    @Test
    public void fromLegacyStringParsesASingleDate() {
        ConferencePeriod period = ConferencePeriod.fromLegacyString("2026-03-12");

        assertEquals("2026-03-12", period.getStart());
        assertEquals("2026-03-12", period.getEnd());
        assertEquals(DatePrecision.DAY, period.getPrecision());
    }

    @Test
    public void fromLegacyStringParsesARange() {
        ConferencePeriod period = ConferencePeriod.fromLegacyString("2026-03-01/2026-03-03");

        assertEquals("2026-03-01", period.getStart());
        assertEquals("2026-03-03", period.getEnd());
        assertEquals(DatePrecision.DAY, period.getPrecision());
    }

    @Test
    public void fromLegacyStringTrimsSpacesAroundRangeBounds() {
        ConferencePeriod period = ConferencePeriod.fromLegacyString("2026-03-01 / 2026-03-03");

        assertEquals("2026-03-01", period.getStart());
        assertEquals("2026-03-03", period.getEnd());
    }

    @Test
    public void fromLegacyStringParsesAMonthAsItsFullSpan() {
        ConferencePeriod period = ConferencePeriod.fromLegacyString("2026-02");

        assertEquals("2026-02-01", period.getStart());
        assertEquals("2026-02-28", period.getEnd());
        assertEquals(DatePrecision.MONTH, period.getPrecision());
    }

    @Test
    public void fromLegacyStringHandlesLeapYearFebruary() {
        ConferencePeriod period = ConferencePeriod.fromLegacyString("2024-02");

        assertEquals("2024-02-29", period.getEnd());
    }

    @Test
    public void fromLegacyStringReturnsNullForNullOrBlankInput() {
        assertNull(ConferencePeriod.fromLegacyString(null));
        assertNull(ConferencePeriod.fromLegacyString(""));
        assertNull(ConferencePeriod.fromLegacyString("   "));
    }

    @Test
    public void fromLegacyStringReturnsNullForUnrecognizedFormat() {
        assertNull(ConferencePeriod.fromLegacyString("not-a-date"));
    }

    @Test
    public void fromLegacyStringReturnsNullForAnInvalidMonth() {
        assertNull(ConferencePeriod.fromLegacyString("2026-13"));
    }

    @Test
    public void fromLegacyStringReturnsNullWhenOneBoundOfARangeIsInvalid() {
        assertNull(ConferencePeriod.fromLegacyString("2026-03-01/not-a-date"));
    }
}
