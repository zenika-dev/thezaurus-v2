package com.zenika.thezaurus.visuals.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.zenika.thezaurus.visuals.model.PersonInput;
import java.io.IOException;

/**
 * Désérialiseur flexible pour {@link PersonInput}.
 * Accepte indifféremment une chaîne simple ("Nom") ou un objet JSON ({"name": "...", "photoBase64": "..."}).
 */
public class PersonInputDeserializer extends JsonDeserializer<PersonInput> {

    @Override
    public PersonInput deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return new PersonInput(p.getText().trim(), null);
        }
        if (p.currentToken() == JsonToken.START_OBJECT) {
            JsonNode node = p.getCodec().readTree(p);
            String name = null;
            if (node.hasNonNull("name")) {
                name = node.get("name").asText().trim();
            } else if (node.hasNonNull("speakerName")) {
                name = node.get("speakerName").asText().trim();
            } else if (node.hasNonNull("authorName")) {
                name = node.get("authorName").asText().trim();
            }

            String photo = null;
            if (node.hasNonNull("photoBase64")) {
                photo = node.get("photoBase64").asText().trim();
            } else if (node.hasNonNull("photo")) {
                photo = node.get("photo").asText().trim();
            } else if (node.hasNonNull("speakerPhotoBase64")) {
                photo = node.get("speakerPhotoBase64").asText().trim();
            } else if (node.hasNonNull("authorPhotoBase64")) {
                photo = node.get("authorPhotoBase64").asText().trim();
            }

            return new PersonInput(name, photo);
        }
        return null;
    }
}
