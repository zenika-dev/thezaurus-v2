package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MonthLabel {
    JANUARY("Jan"),
    FEBRUARY("Fév"),
    MARCH("Mar"),
    APRIL("Avr"),
    MAY("Mai"),
    JUNE("Juin"),
    JULY("Juil"),
    AUGUST("Août"),
    SEPTEMBER("Sep"),
    OCTOBER("Oct"),
    NOVEMBER("Nov"),
    DECEMBER("Déc");

    private final String label;

    MonthLabel(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    public static MonthLabel of(int monthValue) {
        return values()[monthValue - 1];
    }
}
