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
                logo = sanitizeLogo(node.get("logoBase64").asText());
            } else if (node.hasNonNull("logo")) {
                JsonNode logoNode = node.get("logo");
                if (logoNode.isObject()) {
                    if (logoNode.hasNonNull("base64")) {
                        logo = sanitizeLogo(logoNode.get("base64").asText());
                    }
                    if (logo == null && logoNode.hasNonNull("url")) {
                        logo = sanitizeLogo(logoNode.get("url").asText());
                    }
                } else {
                    logo = sanitizeLogo(logoNode.asText());
                }
            } else if (node.hasNonNull("conferenceLogoBase64")) {
                logo = sanitizeLogo(node.get("conferenceLogoBase64").asText());
            }

            return new ConferenceInput(name, logo);
        }
        return null;
    }

    private static String sanitizeLogo(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return (trimmed.isEmpty() || "string".equalsIgnoreCase(trimmed)) ? null : trimmed;
    }
}
