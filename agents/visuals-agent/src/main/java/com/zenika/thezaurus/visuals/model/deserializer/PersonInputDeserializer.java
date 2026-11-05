package com.zenika.thezaurus.visuals.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.zenika.thezaurus.visuals.model.PersonInput;
import com.zenika.thezaurus.visuals.model.PhotoInput;
import java.io.IOException;

/**
 * Désérialiseur flexible pour {@link PersonInput}.
 * Accepte indifféremment une chaîne simple ("Nom") ou un objet JSON
 * ({"name": "...", "photo": {"url": "...", "base64": "..."}}).
 */
public class PersonInputDeserializer extends JsonDeserializer<PersonInput> {

    @Override
    public PersonInput deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return new PersonInput(p.getText().trim(), (PhotoInput) null);
        }
        if (p.currentToken() == JsonToken.START_OBJECT) {
            JsonNode node = p.getCodec().readTree(p);
            String name = node.hasNonNull("name") ? node.get("name").asText().trim() : null;

            String url = null;
            String base64 = null;

            if (node.hasNonNull("photo")) {
                JsonNode photoNode = node.get("photo");
                if (photoNode.isObject()) {
                    if (photoNode.hasNonNull("url")) {
                        url = sanitizeValue(photoNode.get("url").asText());
                    }
                    if (photoNode.hasNonNull("base64")) {
                        base64 = sanitizeValue(photoNode.get("base64").asText());
                    }
                } else if (photoNode.isTextual()) {
                    String rawPhoto = sanitizeValue(photoNode.asText());
                    if (rawPhoto != null) {
                        if (isHttpUrl(rawPhoto)) {
                            url = rawPhoto;
                        } else {
                            base64 = rawPhoto;
                        }
                    }
                }
            }

            if (url == null && node.hasNonNull("photoUrl")) {
                url = sanitizeValue(node.get("photoUrl").asText());
            }
            if (base64 == null && node.hasNonNull("photoBase64")) {
                base64 = sanitizeValue(node.get("photoBase64").asText());
            }

            PhotoInput photo = (url != null || base64 != null) ? new PhotoInput(url, base64) : null;
            return new PersonInput(name, photo);
        }
        return null;
    }

    private static String sanitizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "string".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
