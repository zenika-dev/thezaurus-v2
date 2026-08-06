package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConferenceReach {
    @JsonProperty("Locale") LOCALE,
    @JsonProperty("Régionale") REGIONALE,
    @JsonProperty("Nationale") NATIONALE
}
