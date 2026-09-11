package com.zenika.thezaurus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SearchSanitizerTest {

    @Test
    public void testSanitizeSearchQueryNullAndBlank() {
        assertEquals("", SearchSanitizer.sanitizeSearchQuery(null));
        assertEquals("", SearchSanitizer.sanitizeSearchQuery(""));
        assertEquals("", SearchSanitizer.sanitizeSearchQuery("   "));
    }

    @Test
    public void testSanitizeSearchQueryStripsControlCharacters() {
        assertEquals("Alice", SearchSanitizer.sanitizeSearchQuery("Alice\u0000\u0007\r\n\t"));
        assertEquals("Bob Martin", SearchSanitizer.sanitizeSearchQuery("\u0001Bob\u0000 Martin\u001F"));
    }

    @Test
    public void testSanitizeSearchQueryTruncatesLength() {
        String longInput = "a".repeat(150);
        String result = SearchSanitizer.sanitizeSearchQuery(longInput);
        assertEquals(100, result.length());
        assertEquals("a".repeat(100), result);

        String customBounded = SearchSanitizer.sanitizeSearchQuery("abcdef", 3);
        assertEquals("abc", customBounded);
    }

    @Test
    public void testNormalizeForSearch() {
        assertEquals("", SearchSanitizer.normalizeForSearch(null));
        assertEquals("", SearchSanitizer.normalizeForSearch(""));
        assertEquals("", SearchSanitizer.normalizeForSearch("   "));
        assertEquals("elodie francois", SearchSanitizer.normalizeForSearch("Élodie François"));
        assertEquals("mohamed amine", SearchSanitizer.normalizeForSearch("Mohamed Amine"));
        assertEquals("noel", SearchSanitizer.normalizeForSearch("Noël"));
        assertEquals("alice", SearchSanitizer.normalizeForSearch("Alice\u0000\u0007\r\n\t"));
        assertEquals("elo", SearchSanitizer.normalizeForSearch("Élodie François", 3));
    }

    @Test
    public void testContainsNormalized() {
        org.junit.jupiter.api.Assertions.assertTrue(SearchSanitizer.containsNormalized("Mohamed Amine", "amine"));
        org.junit.jupiter.api.Assertions.assertTrue(SearchSanitizer.containsNormalized("Élodie François", "elodie"));
        org.junit.jupiter.api.Assertions.assertTrue(SearchSanitizer.containsNormalized("Élodie François", "francois"));
        org.junit.jupiter.api.Assertions.assertTrue(SearchSanitizer.containsNormalized("john.doe@zenika.com", "doe"));

        org.junit.jupiter.api.Assertions.assertFalse(SearchSanitizer.containsNormalized("Alice Martin", "bob"));

        org.junit.jupiter.api.Assertions.assertFalse(SearchSanitizer.containsNormalized(null, "amine"));
        org.junit.jupiter.api.Assertions.assertFalse(SearchSanitizer.containsNormalized("Mohamed Amine", null));
        org.junit.jupiter.api.Assertions.assertFalse(SearchSanitizer.containsNormalized("", "amine"));
        org.junit.jupiter.api.Assertions.assertFalse(SearchSanitizer.containsNormalized("Mohamed Amine", "   "));
    }
}
