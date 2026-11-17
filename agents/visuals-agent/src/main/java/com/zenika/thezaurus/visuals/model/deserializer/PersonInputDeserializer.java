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
            var fields = node.fieldNames();
            while (fields.hasNext()) {
                if (!java.util.Set.of(
                                "name",
                                "photo",
                                "photoUrl",
                                "photoBase64",
                                "speakerName",
                                "authorName",
                                "speakerPhotoBase64",
                                "authorPhotoBase64")
                        .contains(fields.next())) {
                    ctxt.reportInputMismatch(PersonInput.class, "Champ de personne inconnu.");
                }
            }
            String name = firstValue(node, "name", "speakerName", "authorName");

            String url = null;
            String base64 = null;

            if (node.hasNonNull("photo")) {
                JsonNode photoNode = node.get("photo");
                if (photoNode.isObject()) {
                    var photoFields = photoNode.fieldNames();
                    while (photoFields.hasNext()) {
                        if (!java.util.Set.of("url", "base64").contains(photoFields.next())) {
                            ctxt.reportInputMismatch(PersonInput.class, "Champ de photo inconnu.");
                        }
                    }
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
            if (base64 == null) base64 = firstValue(node, "photoBase64", "speakerPhotoBase64", "authorPhotoBase64");

            PhotoInput photo = (url != null || base64 != null) ? new PhotoInput(url, base64) : null;
            return new PersonInput(name, photo);
        }
        return null;
    }

    private static String firstValue(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.hasNonNull(key)) {
                String value = sanitizeValue(node.get(key).asText());
                if (value != null) return value;
            }
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
