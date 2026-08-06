package com.zenika.thezaurus.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConferenceType {
    @JsonProperty("Marketing / business") MARKETING_BUSINESS,
    @JsonProperty("Technique stratégique") TECHNIQUE_STRATEGIQUE,
    @JsonProperty("Technique généraliste") TECHNIQUE_GENERALISTE,
    @JsonProperty("Technique") TECHNIQUE,
    @JsonProperty("Hors scope") HORS_SCOPE
}
