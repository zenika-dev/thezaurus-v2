package com.zenika.thezaurus.util;

import com.google.common.base.CharMatcher;
import java.text.Normalizer;

public final class SearchSanitizer {

    public static final int DEFAULT_MAX_QUERY_LENGTH = 100;

    private SearchSanitizer() {}

    public static String sanitizeSearchQuery(String input) {
        return sanitizeSearchQuery(input, DEFAULT_MAX_QUERY_LENGTH);
    }

    public static String sanitizeSearchQuery(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String cleaned = CharMatcher.javaIsoControl().removeFrom(input).trim();
        int boundedMaxLength = Math.max(maxLength, 0);
        return cleaned.length() > boundedMaxLength ? cleaned.substring(0, boundedMaxLength) : cleaned;
    }

    public static String normalizeForSearch(String input) {
        return normalizeForSearch(input, DEFAULT_MAX_QUERY_LENGTH);
    }

    public static String normalizeForSearch(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String sanitized = sanitizeSearchQuery(input, maxLength);
        if (sanitized.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(sanitized, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }

    public static boolean containsNormalized(String candidate, String normalizedQuery) {
        if (candidate == null || candidate.isBlank() || normalizedQuery == null || normalizedQuery.isBlank()) {
            return false;
        }
        return normalizeForSearch(candidate).contains(normalizedQuery);
    }
}
