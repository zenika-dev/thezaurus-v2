package com.zenika.thezaurus.model;

import java.time.LocalDateTime;
import java.util.List;

public class BlogPost {
    private String id;
    private String title;
    private List<String> writers;
    private LocalDateTime creationDate;
    private LocalDateTime publicationDate;
    private String link;

    public BlogPost() {}

    public BlogPost(String id, String title, String link) {
        this.id = id;
        this.title = title;
        this.link = link;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getWriters() { return writers; }
    public void setWriters(List<String> writers) { this.writers = writers; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public LocalDateTime getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDateTime publicationDate) { this.publicationDate = publicationDate; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
}
