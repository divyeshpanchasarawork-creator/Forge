package com.forge.common.util;

/**
 * Single source of truth for fuzzy topic-title ↔ tag/slug/name matching, shared by the
 * scorer, practice attempt linking, and the knowledge graph. A title and a tag refer to
 * the same concept when their normalized word forms contain each other
 * ("dynamic-programming" ↔ "Dynamic Programming", "heap (priority queue)" ↔ "heap").
 */
public final class TitleMatcher {

    private TitleMatcher() {
    }

    /** Lowercase word form: every non-alphanumeric run becomes a single space. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** True when both values are non-blank and either normalized form contains the other. */
    public static boolean topicMatches(String topicTitle, String tagOrSlug) {
        if (topicTitle == null || topicTitle.isBlank() || tagOrSlug == null || tagOrSlug.isBlank()) {
            return false;
        }
        String title = normalize(topicTitle);
        String candidate = normalize(tagOrSlug);
        if (title.isEmpty() || candidate.isEmpty()) {
            return false;
        }
        return title.contains(candidate) || candidate.contains(title);
    }
}
