package com.zenika.thezaurus.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.cloud.firestore.encoding.CustomClassMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TalkTest {

    @Test
    public void testWithIdKeepsOtherFields() {
        Talk talk = new Talk(
                "Titre",
                "Desc",
                List.of(User.builder().name("Jane").build()),
                "Nantes",
                TalkStatus.DRAFT,
                Visibility.PUBLIC);

        Talk withId = talk.withId("42");

        assertEquals("42", withId.id());
        assertEquals("Titre", withId.title());
        assertEquals("Jane", withId.speakers().get(0).name());
        assertEquals(TalkStatus.DRAFT, withId.status());
    }

    @Test
    public void testWithConferenceKeepsOtherFields() {
        Talk talk = new Talk("1", "Titre", "Desc");

        Talk withConference = talk.withConference(new Conference(null, "Devoxx", null));

        assertEquals("1", withConference.id());
        assertEquals("Devoxx", withConference.conference().getName());
    }

    // --- Mapping Firestore ---------------------------------------------------------------------
    // Talk est un record : le SDK passe par RecordMapper (constructeur canonique / accesseurs).

    private Talk deserialize(Object speakers) {
        Map<String, Object> document = new HashMap<>();
        document.put("title", "Un talk");
        document.put("speakers", speakers);
        return CustomClassMapper.convertToCustomClass(document, Talk.class, null);
    }

    @Test
    public void testFirestoreDeserializationOfSpeakers() {
        Talk talk = deserialize(List.of(Map.of(
                "name", "Jane",
                "email", "jane@zenika.com",
                "slackUserId", "U123")));

        assertEquals("Un talk", talk.title());
        assertEquals("Jane", talk.speakers().get(0).name());
        assertEquals("jane@zenika.com", talk.speakers().get(0).email());
        assertEquals("U123", talk.speakers().get(0).slackUserId());
    }

    @Test
    public void testFirestoreDeserializationWithoutSpeakers() {
        Talk talk = deserialize(null);

        assertEquals("Un talk", talk.title());
        assertNull(talk.speakers());
    }

    @Test
    public void testFirestoreSerializationOfSpeakers() {
        Talk talk = new Talk(
                "Titre",
                "Desc",
                List.of(User.builder()
                        .name("Jane")
                        .email("jane@zenika.com")
                        .slackUserId("U123")
                        .build()),
                "Nantes",
                TalkStatus.DRAFT,
                Visibility.PUBLIC);

        @SuppressWarnings("unchecked")
        Map<String, Object> serialized = (Map<String, Object>) CustomClassMapper.serialize(talk);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> speakers = (List<Map<String, Object>>) serialized.get("speakers");

        assertEquals("Jane", speakers.get(0).get("name"));
        assertEquals("jane@zenika.com", speakers.get(0).get("email"));
        assertEquals("U123", speakers.get(0).get("slackUserId"));
    }
}
