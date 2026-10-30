package com.zenika.thezaurus.visuals.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.zenika.thezaurus.visuals.model.ConferenceInput;
import java.io.IOException;

/**
 * Désérialiseur flexible pour {@link ConferenceInput}.
 * Accepte indifféremment une chaîne simple ("Nom Conférence") ou un objet JSON ({"name": "...", "logoBase64": "..."}).
 */
public class ConferenceInputDeserializer extends JsonDeserializer<ConferenceInput> {

    @Override
    public ConferenceInput deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return new ConferenceInput(p.getText().trim(), null);
        }
        if (p.currentToken() == JsonToken.START_OBJECT) {
            JsonNode node = p.getCodec().readTree(p);
            String name = null;
            if (node.hasNonNull("name")) {
                name = node.get("name").asText().trim();
            } else if (node.hasNonNull("conferenceName")) {
                name = node.get("conferenceName").asText().trim();
            }

            String logo = null;
            if (node.hasNonNull("logoBase64")) {
                logo = node.get("logoBase64").asText().trim();
            } else if (node.hasNonNull("logo")) {
                logo = node.get("logo").asText().trim();
            } else if (node.hasNonNull("conferenceLogoBase64")) {
                logo = node.get("conferenceLogoBase64").asText().trim();
            }

            return new ConferenceInput(name, logo);
        }
        return null;
    }
}
