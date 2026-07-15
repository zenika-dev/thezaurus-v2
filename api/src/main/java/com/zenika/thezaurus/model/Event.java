package com.zenika.thezaurus.model;

public record Event(String id, String name, String type, String date, Visibility visibility, Location location) {

    public Event(String id, String name, String type) {
        this(id, name, type, null, null, null);
    }

    public Event withId(String id) {
        return new Event(id, name, type, date, visibility, location);
    }
}
