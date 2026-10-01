package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.BlogPostStatus;
import com.zenika.thezaurus.repository.BlogPostRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class BlogPostService {

    @Inject
    BlogPostRepository repository;

    @Inject
    Clock clock;

    public List<BlogPost> findAll() throws ExecutionException, InterruptedException {
        return repository.findAll();
    }

    public BlogPost findById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public BlogPost create(BlogPost blogPost) throws ExecutionException, InterruptedException {
        validate(blogPost);
        return repository.create(blogPost);
    }

    public BlogPost update(String id, BlogPost blogPost) throws ExecutionException, InterruptedException {
        validate(blogPost);
        BlogPost existing = repository.findById(id);
        if (existing == null) {
            return null;
        }
        return repository.update(id, blogPost);
    }

    public boolean delete(String id) throws ExecutionException, InterruptedException {
        BlogPost existing = repository.findById(id);
        if (existing == null) {
            return false;
        }
        repository.delete(id);
        return true;
    }

    private void validate(BlogPost post) {
        if (post == null) throw new BadRequestException("Un article est requis");
        validateDate(post.getCreationDate(), "La date de création", true);
        validateDate(post.getPublicationDate(), "La date de publication prévue", false);
        validateDate(post.getActualPublicationDate(), "La date de publication réelle", true);
        if ((post.getStatus() == BlogPostStatus.REVIEW || post.getStatus() == BlogPostStatus.READY_TO_PUBLISH)
                && (post.getGoogleDocDraftLink() == null
                        || post.getGoogleDocDraftLink().isBlank())) {
            throw new BadRequestException("Le lien du texte à relire est requis");
        }
        if (post.getStatus() == BlogPostStatus.PUBLISHED) {
            if (post.getLink() == null || post.getLink().isBlank()) {
                throw new BadRequestException("L’URL publique est requise");
            }
            if (post.getActualPublicationDate() == null
                    || post.getActualPublicationDate().isBlank()) {
                throw new BadRequestException("La date de publication réelle est requise");
            }
        }
    }

    private void validateDate(String value, String label, boolean pastOrPresent) {
        if (value == null || value.isEmpty()) return;
        LocalDate date;
        try {
            date = value.length() == 10
                    ? LocalDate.parse(value)
                    : LocalDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new BadRequestException(label + " est invalide");
        }
        if (pastOrPresent && date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException(label + " ne peut pas être future");
        }
    }
}
