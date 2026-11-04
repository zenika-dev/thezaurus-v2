package com.zenika.thezaurus.model;

import java.util.List;

public record ContributionStatistics(
        int currentYear, List<AnnualContributions> years, int undatedCandidatures, int undatedPublishedArticles) {
    public record AnnualContributions(
            int year, int publishedArticles, int submittedCandidatures, int acceptedCandidatures) {}
}
