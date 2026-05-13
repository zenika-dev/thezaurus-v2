package com.zenika.thezaurus.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.BlogPost;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class BlogPostRepository {

    @Inject
    Firestore firestore;

    private static final String COLLECTION_NAME = "blog_posts";

    public List<BlogPost> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(BlogPost.class))
                .collect(Collectors.toList());
    }

    public BlogPost findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(BlogPost.class);
        }
        return null;
    }

    public BlogPost create(BlogPost blogPost) throws ExecutionException, InterruptedException {
        if (blogPost.getId() == null || blogPost.getId().isEmpty()) {
            blogPost.setId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(blogPost.getId());
        ApiFuture<WriteResult> result = docRef.set(blogPost);
        result.get();
        return blogPost;
    }

    public BlogPost update(String id, BlogPost blogPost) throws ExecutionException, InterruptedException {
        blogPost.setId(id);
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> result = docRef.set(blogPost);
        result.get();
        return blogPost;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult = firestore.collection(COLLECTION_NAME).document(id).delete();
        writeResult.get();
    }
}
