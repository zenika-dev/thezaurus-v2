package com.zenika.thezaurus.model;

import java.util.List;

public class Talk {
    private String id;
    private String title;
    private String description;
    private List<String> speakers;
    private String office;
    private Conference conference;
    private TalkStatus status;
    private Visibility visibility;

    public Talk() {}

    public Talk(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getSpeakers() { return speakers; }
    public void setSpeakers(List<String> speakers) { this.speakers = speakers; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public Conference getConference() { return conference; }
    public void setConference(Conference conference) { this.conference = conference; }

    public TalkStatus getStatus() { return status; }
    public void setStatus(TalkStatus status) { this.status = status; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
}
