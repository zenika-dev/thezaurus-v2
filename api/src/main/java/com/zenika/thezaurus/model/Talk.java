package com.zenika.thezaurus.model;

import java.util.List;

public record Talk(
        String id,
        String title,
        String description,
        List<User> speakers,
        String office,
        Conference conference,
        TalkStatus status,
        Visibility visibility) {

    public Talk(String id, String title, String description) {
        this(id, title, description, null, null, null, null, null);
    }

    public Talk(
            String title,
            String description,
            List<User> speakers,
            String office,
            TalkStatus status,
            Visibility visibility) {
        this(null, title, description, speakers, office, null, status, visibility);
    }

    public Talk withId(String id) {
        return new Talk(id, title, description, speakers, office, conference, status, visibility);
    }

    public Talk withConference(Conference conference) {
        return new Talk(id, title, description, speakers, office, conference, status, visibility);
    }
}
