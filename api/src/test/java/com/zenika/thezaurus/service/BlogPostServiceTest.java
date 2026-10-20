package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.BlogPostStatus;
import com.zenika.thezaurus.model.PagedResult;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.BlogPostRepository;
import io.quarkus.security.identity.SecurityIdentity;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlogPostServiceTest {

    private BlogPostRepository repository;
    private SecurityIdentity identity;
    private BlogPostService service;

    @BeforeEach
    void setUp() {
        repository = mock(BlogPostRepository.class);
        identity = mock(SecurityIdentity.class);
        service = new BlogPostService();
        service.repository = repository;
        service.identity = identity;
        service.clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("UTC"));
    }

    private BlogPost createPost(
            String id,
            String title,
            String creationDate,
            String actualPubDate,
            BlogPostStatus status,
            String writerEmail) {
        BlogPost post = new BlogPost(id, title, "https://blog.zenika.com/article");
        post.setCreationDate(creationDate);
        post.setActualPublicationDate(actualPubDate);
        post.setStatus(status);
        post.setWriters(List.of(User.builder().name("Writer").email(writerEmail).build()));
        post.setTags(Collections.emptyList());
        return post;
    }

    @Test
    void findPagedDelegatesToRepositoryWithoutPersonalFilter() throws Exception {
        BlogPost p1 = createPost("1", "Post A", "2026-03-01", null, BlogPostStatus.IDEA, "a@zenika.com");
        PagedResult<BlogPost> expected = new PagedResult<>(List.of(p1), 1, 0, 25, 1);
        when(repository.findPaged(0, 25, BlogPostStatus.IDEA, null)).thenReturn(expected);

        PagedResult<BlogPost> result = service.findPaged(0, 25, BlogPostStatus.IDEA, false);

        assertEquals(expected, result);
        verify(repository).findPaged(0, 25, BlogPostStatus.IDEA, null);
    }

    @Test
    void findPagedPassesCurrentUserEmailWhenPersonalIsTrue() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("dev@zenika.com");
        when(identity.getPrincipal()).thenReturn(principal);

        BlogPost p1 = createPost("1", "Post 1", "2026-01-01", null, BlogPostStatus.IDEA, "dev@zenika.com");
        PagedResult<BlogPost> expected = new PagedResult<>(List.of(p1), 1, 0, 25, 1);
        when(repository.findPaged(0, 25, BlogPostStatus.IDEA, "dev@zenika.com")).thenReturn(expected);

        PagedResult<BlogPost> result = service.findPaged(0, 25, BlogPostStatus.IDEA, true);

        assertEquals(expected, result);
        verify(repository).findPaged(0, 25, BlogPostStatus.IDEA, "dev@zenika.com");
    }
}
