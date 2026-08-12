package com.zenika.thezaurus.model;

import com.google.cloud.firestore.encoding.CustomClassMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TalkTest {

    @Test
    public void testSetSpeakersKeepsTheGivenList() {
        User jane = User.builder().name("Jane").email("jane@zenika.com").build();
        Talk talk = new Talk();
        talk.setSpeakers(List.of(jane));

        assertEquals(1, talk.getSpeakers().size());
        assertSame(jane, talk.getSpeakers().get(0));
    }

    @Test
    public void testSetSpeakersNullIsNull() {
        Talk talk = new Talk();
        talk.setSpeakers(null);
        assertNull(talk.getSpeakers());
    }

    // --- Mapping Firestore ---------------------------------------------------------------------
    // Le setter est typé, le SDK Firestore fait donc lui-même le mapping des speakers.

    private Talk deserialize(Object speakers) {
        Map<String, Object> document = new HashMap<>();
        document.put("title", "Un talk");
        document.put("speakers", speakers);
        return CustomClassMapper.convertToCustomClass(document, Talk.class, null);
    }

    @Test
    public void testFirestoreDeserializationOfStructuredSpeakers() {
        Talk talk = deserialize(List.of(Map.of(
                "name", "Jane",
                "email", "jane@zenika.com",
                "slackUserId", "U123")));

        assertEquals("Jane", talk.getSpeakers().get(0).getName());
        assertEquals("jane@zenika.com", talk.getSpeakers().get(0).getEmail());
        assertEquals("U123", talk.getSpeakers().get(0).getSlackUserId());
    }

    @Test
    public void testFirestoreSerializationOfSpeakers() {
        Talk talk = new Talk();
        talk.setSpeakers(List.of(User.builder()
                .name("Jane").email("jane@zenika.com").slackUserId("U123").build()));

        @SuppressWarnings("unchecked")
        Map<String, Object> serialized = (Map<String, Object>) CustomClassMapper.serialize(talk);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> speakers = (List<Map<String, Object>>) serialized.get("speakers");

        assertEquals("Jane", speakers.get(0).get("name"));
        assertEquals("jane@zenika.com", speakers.get(0).get("email"));
        assertEquals("U123", speakers.get(0).get("slackUserId"));
    }
}
