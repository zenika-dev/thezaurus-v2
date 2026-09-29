package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Locale;

public enum Office {
    @JsonProperty("paris")
    PARIS,
    @JsonProperty("nantes")
    NANTES,
    @JsonProperty("rennes")
    RENNES,
    @JsonProperty("bordeaux")
    BORDEAUX,
    @JsonProperty("lyon")
    LYON,
    @JsonProperty("lille")
    LILLE,
    @JsonProperty("grenoble")
    GRENOBLE,
    @JsonProperty("singapour")
    SINGAPOUR,
    @JsonProperty("montreal")
    MONTREAL;

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return this == MONTREAL ? "Montréal" : name().charAt(0) + value().substring(1);
    }

    /** La chaîne vide historique représente une agence non renseignée, pas une agence. */
    @JsonCreator
    public static Office fromValue(String value) {
        if (value == null || value.isEmpty()) return null;
        return Arrays.stream(values())
                .filter(office -> office.value().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Agence inconnue : " + value));
    }
}
