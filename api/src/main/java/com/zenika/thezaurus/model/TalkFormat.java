package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Locale;

/** Formats proposés pour les talks ; les anciennes valeurs restent lisibles sans migration. */
public enum TalkFormat {
    @JsonProperty("quicky")
    QUICKY,
    @JsonProperty("conference")
    CONFERENCE,
    @JsonProperty("workshop")
    WORKSHOP;

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static boolean isSupported(String value) {
        return Arrays.stream(values()).anyMatch(format -> format.value().equals(value));
    }
}
