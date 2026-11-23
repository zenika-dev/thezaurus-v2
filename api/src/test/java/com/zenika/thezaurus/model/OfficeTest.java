package com.zenika.thezaurus.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.junit.jupiter.api.Test;

class OfficeTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsLegacyEnumNamesAndWritesLowercaseValues() throws Exception {
        for (Office office : Office.values()) {
            assertEquals(office, mapper.readValue("\"" + office.name() + "\"", Office.class));
            String canonicalJson = "\"" + office.value() + "\"";
            assertEquals(office, mapper.readValue(canonicalJson, Office.class));
            assertEquals(canonicalJson, mapper.writeValueAsString(office));
        }
    }

    @Test
    void handlesMissingAndUnknownOffices() throws Exception {
        assertNull(mapper.readValue("\"\"", Office.class));
        assertNull(mapper.readValue("null", Office.class));
        assertThrows(ValueInstantiationException.class, () -> mapper.readValue("\"inconnue\"", Office.class));
    }
}
