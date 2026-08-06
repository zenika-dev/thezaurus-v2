package com.zenika.thezaurus.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TalkTest {

    @Test
    public void testSetSpeakersFromLegacyStringList() {
        Talk talk = new Talk();
        talk.setSpeakers(List.of("Jane Doe"));

        assertEquals(1, talk.getSpeakers().size());
        assertEquals("Jane Doe", talk.getSpeakers().get(0).getName());
        assertNull(talk.getSpeakers().get(0).getEmail());
    }

    @Test
    public void testSetSpeakersFromMapList() {
        Talk talk = new Talk();
        talk.setSpeakers(List.of(Map.of("name", "Jane", "email", "jane@zenika.com")));

        assertEquals(1, talk.getSpeakers().size());
        assertEquals("Jane", talk.getSpeakers().get(0).getName());
        assertEquals("jane@zenika.com", talk.getSpeakers().get(0).getEmail());
    }

    @Test
    public void testSetSpeakersPassthroughUserList() {
        User jane = User.builder().name("Jane").email("jane@zenika.com").build();
        Talk talk = new Talk();
        talk.setSpeakers(List.of(jane));

        assertSame(jane, talk.getSpeakers().get(0));
    }

    @Test
    public void testSetSpeakersRejectsUnknownShape() {
        Talk talk = new Talk();
        assertThrows(IllegalArgumentException.class, () -> talk.setSpeakers(List.of(42)));
    }

    @Test
    public void testSetSpeakersNullIsNull() {
        Talk talk = new Talk();
        talk.setSpeakers(null);
        assertNull(talk.getSpeakers());
    }
}
