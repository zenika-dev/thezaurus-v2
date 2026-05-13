package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.repository.BlogPostRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class BlogPostService {

    @Inject
    BlogPostRepository repository;

    public List<BlogPost> findAll() throws ExecutionException, InterruptedException {
        return repository.findAll();
    }

    public BlogPost findById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public BlogPost create(BlogPost blogPost) throws ExecutionException, InterruptedException {
        return repository.create(blogPost);
    }

    public BlogPost update(String id, BlogPost blogPost) throws ExecutionException, InterruptedException {
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
}
