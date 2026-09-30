package com.zenika.thezaurus.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class BlogPost {
    private String id;

    @NotBlank
    private String title;

    @NotEmpty
    private List<@NotNull @Valid User> writers;

    private String creationDate;
    private String publicationDate;
    private String actualPublicationDate;

    private Office office;

    @Pattern(regexp = "^$|https?://[^\\s]+", message = "Une URL HTTP ou HTTPS est requise")
    private String link;

    @Pattern(regexp = "^$|https?://[^\\s]+", message = "Une URL HTTP ou HTTPS est requise")
    private String googleDocDraftLink;

    @NotNull
    private BlogPostStatus status;

    private List<String> tags;

    public BlogPost() {}

    public BlogPost(String id, String title, String link) {
        this.id = id;
        this.title = title;
        this.link = link;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<User> getWriters() {
        return writers;
    }

    public void setWriters(List<User> writers) {
        this.writers = writers;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getActualPublicationDate() {
        return actualPublicationDate;
    }

    public void setActualPublicationDate(String actualPublicationDate) {
        this.actualPublicationDate = actualPublicationDate;
    }

    public Office getOffice() {
        return office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    /** Lien vers le brouillon Google Doc de l'article avant publication. */
    public String getGoogleDocDraftLink() {
        return googleDocDraftLink;
    }

    public void setGoogleDocDraftLink(String googleDocDraftLink) {
        this.googleDocDraftLink = googleDocDraftLink;
    }

    public BlogPostStatus getStatus() {
        return status;
    }

    public void setStatus(BlogPostStatus status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
