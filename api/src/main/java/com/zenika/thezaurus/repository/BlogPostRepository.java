package com.zenika.thezaurus.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.BlogPost;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class BlogPostRepository {

    @Inject
    Firestore firestore;

    @Inject
    ObjectMapper mapper;

    @Inject
    @ConfigProperty(name = "thezaurus.firestore.collection.prefix")
    Optional<String> collectionPrefix;

    private static final String BASE_COLLECTION_NAME = "blog_posts";

    private String getCollectionName() {
        if (collectionPrefix == null
                || collectionPrefix.isEmpty()
                || collectionPrefix.get().trim().isEmpty()) {
            return BASE_COLLECTION_NAME;
        }
        return collectionPrefix.get().trim() + "_" + BASE_COLLECTION_NAME;
    }

    public List<BlogPost> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query =
                firestore.collection(getCollectionName()).get();
        QuerySnapshot querySnapshot = query.get();
        return querySnapshot.getDocuments().stream().map(this::readPost).collect(Collectors.toList());
    }

    public BlogPost findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return readPost(document);
        }
        return null;
    }

    /** Les noms historiques restent des auteurs sans email, sans réécriture à la lecture. */
    private BlogPost readPost(DocumentSnapshot document) {
        Map<String, Object> data = new HashMap<>(document.getData());
        if (data.get("writers") instanceof List<?> writers) {
            data.put(
                    "writers",
                    writers.stream()
                            .map(writer -> writer instanceof String name ? Map.of("name", name) : writer)
                            .toList());
        }
        data.put("id", document.getId());
        return mapper.convertValue(data, BlogPost.class);
    }

    public BlogPost create(BlogPost blogPost) throws ExecutionException, InterruptedException {
        if (blogPost.getId() == null || blogPost.getId().isEmpty()) {
            blogPost.setId(UUID.randomUUID().toString());
        }
        DocumentReference docRef = firestore.collection(getCollectionName()).document(blogPost.getId());
        ApiFuture<WriteResult> result = docRef.set(blogPost);
        result.get();
        return blogPost;
    }

    public BlogPost update(String id, BlogPost blogPost) throws ExecutionException, InterruptedException {
        blogPost.setId(id);
        DocumentReference docRef = firestore.collection(getCollectionName()).document(id);
        ApiFuture<WriteResult> result = docRef.set(blogPost);
        result.get();
        return blogPost;
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> writeResult =
                firestore.collection(getCollectionName()).document(id).delete();
        writeResult.get();
    }
}
