package com.zenika.thezaurus.model;

public class Conference {
    private String id;
    private String name;
    private String date;
    private String cfpLink;
    private Location location;

    public Conference() {}

    public Conference(String id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCfpLink() { return cfpLink; }
    public void setCfpLink(String cfpLink) { this.cfpLink = cfpLink; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
}
