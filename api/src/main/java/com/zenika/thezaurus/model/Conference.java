package com.zenika.thezaurus.model;

public class Conference {
    private String id;
    private String name;
    private String date;
    private String cfpLink;
    private Location location;
    private String cfpStatus;
    private int submittedTalksAmount;
    private String cfpClosingDate;
    private String type;
    private String reach;

    public Conference() {
    }

    public Conference(String id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCfpLink() {
        return cfpLink;
    }

    public void setCfpLink(String cfpLink) {
        this.cfpLink = cfpLink;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getCfpStatus() {
        return cfpStatus;
    }

    public void setCfpStatus(String cfpStatus) {
        this.cfpStatus = cfpStatus;
    }

    public int getSubmittedTalksAmount() {
        return submittedTalksAmount;
    }

    public void setSubmittedTalksAmount(int submittedTalksAmount) {
        this.submittedTalksAmount = submittedTalksAmount;
    }

    public String getCfpClosingDate() {
        return cfpClosingDate;
    }

    public void setCfpClosingDate(String cfpClosingDate) {
        this.cfpClosingDate = cfpClosingDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReach() {
        return reach;
    }

    public void setReach(String reach) {
        this.reach = reach;
    }
}
