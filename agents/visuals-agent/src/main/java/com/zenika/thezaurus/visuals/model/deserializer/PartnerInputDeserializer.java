package com.zenika.thezaurus.visuals.model.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.zenika.thezaurus.visuals.model.PartnerInput;
import java.io.IOException;

/**
 * Désérialiseur flexible pour {@link PartnerInput}.
 * Accepte indifféremment une chaîne simple ("Nom Partenaire") ou un objet JSON ({"name": "...", "logoBase64": "..."}).
 */
public class PartnerInputDeserializer extends JsonDeserializer<PartnerInput> {

    @Override
    public PartnerInput deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return new PartnerInput(p.getText().trim(), null);
        }
        if (p.currentToken() == JsonToken.START_OBJECT) {
            JsonNode node = p.getCodec().readTree(p);
            String name = null;
            if (node.hasNonNull("name")) {
                name = node.get("name").asText().trim();
            } else if (node.hasNonNull("partnerName")) {
                name = node.get("partnerName").asText().trim();
            }

            String logo = null;
            if (node.hasNonNull("logoBase64")) {
                logo = node.get("logoBase64").asText().trim();
            } else if (node.hasNonNull("logo")) {
                logo = node.get("logo").asText().trim();
            } else if (node.hasNonNull("partnerLogoBase64")) {
                logo = node.get("partnerLogoBase64").asText().trim();
            }

            return new PartnerInput(name, logo);
        }
        return null;
    }
}
