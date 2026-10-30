package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.BlogPostStatus;
import com.zenika.thezaurus.model.ContributionStatistics;
import com.zenika.thezaurus.model.ContributionStatistics.AnnualContributions;
import com.zenika.thezaurus.model.TalkStatus;
import com.zenika.thezaurus.model.User;
import com.zenika.thezaurus.repository.BlogPostRepository;
import com.zenika.thezaurus.repository.TalkRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class ContributionStatisticsService {
    @Inject
    TalkRepository talks;

    @Inject
    BlogPostRepository posts;

    @Inject
    Clock clock;

    public ContributionStatistics forUser(String email) throws ExecutionException, InterruptedException {
        int currentYear = LocalDate.now(clock).getYear();
        Map<Integer, AnnualContributions> years = new TreeMap<>(Comparator.reverseOrder());
        years.put(currentYear, emptyYear(currentYear));
        int undatedCandidatures = 0;
        int undatedArticles = 0;
        for (var talk : talks.findAll()) {
            if (talk == null || !belongsTo(talk.speakers(), email) || talk.conference() == null) continue;
            TalkStatus status = talk.status();
            if (status != TalkStatus.SUBMITTED
                    && status != TalkStatus.ACCEPTED
                    && status != TalkStatus.REJECTED
                    && status != TalkStatus.DONE) continue;
            Integer year = yearOf(talk.date(), false);
            if (year == null) {
                undatedCandidatures++;
                continue;
            }
            var counts = years.getOrDefault(year, emptyYear(year));
            int accepted = status == TalkStatus.ACCEPTED || status == TalkStatus.DONE ? 1 : 0;
            years.put(
                    year,
                    new AnnualContributions(
                            year,
                            counts.publishedArticles(),
                            counts.submittedCandidatures() + 1,
                            counts.acceptedCandidatures() + accepted));
        }
        for (var post : posts.findAll()) {
            if (post == null || post.getStatus() != BlogPostStatus.PUBLISHED || !belongsTo(post.getWriters(), email))
                continue;
            Integer year = yearOf(post.getActualPublicationDate(), true);
            if (year == null) {
                undatedArticles++;
                continue;
            }
            var counts = years.getOrDefault(year, emptyYear(year));
            years.put(
                    year,
                    new AnnualContributions(
                            year,
                            counts.publishedArticles() + 1,
                            counts.submittedCandidatures(),
                            counts.acceptedCandidatures()));
        }
        return new ContributionStatistics(
                currentYear, List.copyOf(years.values()), undatedCandidatures, undatedArticles);
    }

    private boolean belongsTo(List<User> contributors, String email) {
        return email != null
                && !email.isBlank()
                && contributors != null
                && contributors.stream()
                        .anyMatch(user -> user != null
                                && user.email() != null
                                && user.email().trim().equalsIgnoreCase(email.trim()));
    }

    private AnnualContributions emptyYear(int year) {
        return new AnnualContributions(year, 0, 0, 0);
    }

    private Integer yearOf(String value, boolean allowDateTime) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate date = allowDateTime && value.length() != 10
                    ? LocalDateTime.parse(value).toLocalDate()
                    : LocalDate.parse(value);
            return date.getYear() >= 1 && date.getYear() <= 9999 ? date.getYear() : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
