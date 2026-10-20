package com.zenika.thezaurus.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.BlogPostStatus;
import com.zenika.thezaurus.model.PagedResult;
import com.zenika.thezaurus.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
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

    public PagedResult<BlogPost> findPaged(int page, int pageSize, BlogPostStatus status, String writerEmail)
            throws ExecutionException, InterruptedException {
        Query query = firestore.collection(getCollectionName());
        if (status != null) {
            query = query.whereEqualTo("status", status.name());
        }
        QuerySnapshot querySnapshot = query.get().get();
        List<BlogPost> matchingPosts = new ArrayList<>();
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            BlogPost post = readPost(doc);
            if (post != null && matchesWriter(post, writerEmail)) {
                matchingPosts.add(post);
            }
        }
        matchingPosts.sort(this::compareBlogPosts);
        return PagedResult.of(matchingPosts, page, pageSize);
    }

    private boolean matchesWriter(BlogPost post, String email) {
        if (email == null || email.isBlank()) {
            return true;
        }
        if (post.getWriters() == null) {
            return false;
        }
        for (User writer : post.getWriters()) {
            if (writer != null && isSameEmail(writer.email(), email)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameEmail(String email1, String email2) {
        return email1 != null && email1.trim().equalsIgnoreCase(email2.trim());
    }

    private int compareBlogPosts(BlogPost p1, BlogPost p2) {
        String date1 = resolveSortDate(p1);
        String date2 = resolveSortDate(p2);

        int dateComparison = compareDatesDescNullsLast(date1, date2);
        if (dateComparison != 0) {
            return dateComparison;
        }

        String title1 = p1 != null && p1.getTitle() != null ? p1.getTitle() : "";
        String title2 = p2 != null && p2.getTitle() != null ? p2.getTitle() : "";
        return String.CASE_INSENSITIVE_ORDER.compare(title1, title2);
    }

    private String resolveSortDate(BlogPost post) {
        if (post == null) return null;
        if (post.getActualPublicationDate() != null
                && !post.getActualPublicationDate().isBlank()) {
            return post.getActualPublicationDate().trim();
        }
        if (post.getPublicationDate() != null && !post.getPublicationDate().isBlank()) {
            return post.getPublicationDate().trim();
        }
        if (post.getCreationDate() != null && !post.getCreationDate().isBlank()) {
            return post.getCreationDate().trim();
        }
        return null;
    }

    private int compareDatesDescNullsLast(String d1, String d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return 1;
        if (d2 == null) return -1;
        return d2.compareTo(d1);
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
