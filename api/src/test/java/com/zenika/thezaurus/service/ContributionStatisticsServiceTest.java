package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenika.thezaurus.model.BlogPost;
import com.zenika.thezaurus.model.BlogPostStatus;
import com.zenika.thezaurus.model.Conference;
import com.zenika.thezaurus.model.ConferencePeriod;
import com.zenika.thezaurus.model.ContributionStatistics.AnnualContributions;
import com.zenika.thezaurus.model.Talk;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.BlogPostRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContributionStatisticsServiceTest {
    private final ContributionStatisticsService service = new ContributionStatisticsService();
    private static final String EMAIL = "jane@zenika.com";

    @BeforeEach
    void setup() throws Exception {
        service.talks = mock(TalkRepository.class);
        service.posts = mock(BlogPostRepository.class);
        service.clock = Clock.fixed(Instant.parse("2025-12-31T23:30:00Z"), ZoneId.of("Europe/Paris"));
        when(service.talks.findAll()).thenReturn(List.of());
        when(service.posts.findAll()).thenReturn(List.of());
    }

    @Test
    void includesCompletedAndRejectedSubmissionsButOnlyAcceptedAndCompletedAcceptances() throws Exception {
        when(service.talks.findAll())
                .thenReturn(Arrays.stream(TalkStatus.values())
                        .map(status -> talk(status, "2025-12-31", EMAIL))
                        .toList());
        var result = service.forUser(EMAIL);
        assertEquals(2026, result.currentYear());
        assertEquals(
                List.of(new AnnualContributions(2026, 0, 0, 0), new AnnualContributions(2025, 0, 4, 2)),
                result.years());
    }

    @Test
    void countsWholeSharedContributionsOnceAndNeverMatchesOnlyNames() throws Exception {
        var shared = talk(TalkStatus.ACCEPTED, "2027-01-01", " JANE@ZENIKA.COM ");
        when(service.talks.findAll())
                .thenReturn(List.of(
                        shared,
                        shared.withSpeakers(List.of(User.builder().name("Jane").build())),
                        talk(TalkStatus.SUBMITTED, "2027-01-01", "other@zenika.com")));
        var article = post(BlogPostStatus.PUBLISHED, "2025-01-01T00:00:00", " JANE@ZENIKA.COM ");
        article.setCreationDate("2024-01-01");
        article.setPublicationDate("2024-12-31");
        when(service.posts.findAll())
                .thenReturn(List.of(
                        article,
                        post(BlogPostStatus.DRAFT, "2025-01-01", EMAIL),
                        post(BlogPostStatus.PUBLISHED, "2025-01-01", "other@zenika.com")));
        assertEquals(
                List.of(
                        new AnnualContributions(2027, 0, 1, 1),
                        new AnnualContributions(2026, 0, 0, 0),
                        new AnnualContributions(2025, 1, 0, 0)),
                service.forUser(EMAIL).years());
        assertEquals(
                List.of(new AnnualContributions(2026, 0, 0, 0)),
                service.forUser("unknown@zenika.com").years());
    }

    @Test
    void reportsOnlyEligibleUndatedContributionsAndNeverFallsBackToOtherDates() throws Exception {
        var conference =
                new Conference("conference", "Conference", new ConferencePeriod("2025-01-01", "2025-01-02", null));
        when(service.talks.findAll())
                .thenReturn(List.of(
                        talk(TalkStatus.ACCEPTED, null, EMAIL).withConference(conference),
                        talk(TalkStatus.SUBMITTED, "2025-02-30", EMAIL),
                        talk(TalkStatus.DRAFT, null, EMAIL),
                        talk(TalkStatus.SUBMITTED, null, EMAIL).withConference(null),
                        talk(TalkStatus.SUBMITTED, "2025-01-01", EMAIL).withConference(null)));
        var article = post(BlogPostStatus.PUBLISHED, null, EMAIL);
        article.setPublicationDate("2025-01-01");
        when(service.posts.findAll())
                .thenReturn(List.of(
                        article,
                        post(BlogPostStatus.PUBLISHED, "garbage", EMAIL),
                        post(BlogPostStatus.IDEA, null, EMAIL)));
        var result = service.forUser(EMAIL);
        assertEquals(2, result.undatedCandidatures());
        assertEquals(2, result.undatedPublishedArticles());
        assertEquals(List.of(new AnnualContributions(2026, 0, 0, 0)), result.years());
    }

    @Test
    void recalculatesAfterCorrectionsAndDeletion() throws Exception {
        when(service.talks.findAll())
                .thenReturn(
                        List.of(talk(TalkStatus.ACCEPTED, "2025-01-01", EMAIL)),
                        List.of(talk(TalkStatus.REJECTED, "2025-01-01", EMAIL)),
                        List.of());
        assertEquals(
                new AnnualContributions(2025, 0, 1, 1),
                service.forUser(EMAIL).years().get(1));
        assertEquals(
                new AnnualContributions(2025, 0, 1, 0),
                service.forUser(EMAIL).years().get(1));
        assertEquals(
                List.of(new AnnualContributions(2026, 0, 0, 0)),
                service.forUser(EMAIL).years());
    }

    private Talk talk(TalkStatus status, String date, String email) {
        var user = User.builder().email(email).build();
        return new Talk(
                "id",
                "Title",
                "Description",
                List.of(user, user),
                "nantes",
                new Conference("conference", "Conference", null),
                status,
                null,
                "",
                date,
                null,
                null,
                null,
                null,
                null);
    }

    private BlogPost post(BlogPostStatus status, String date, String email) {
        var post = new BlogPost("id", "Title", "https://example.com");
        post.setStatus(status);
        post.setActualPublicationDate(date);
        post.setWriters(List.of(User.builder().email(email).build()));
        return post;
    }
}
