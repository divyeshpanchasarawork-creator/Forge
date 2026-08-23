package com.forge.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TitleMatcherTest {

    @Test
    void matchesSlugAgainstTitleInBothDirections() {
        assertTrue(TitleMatcher.topicMatches("Dynamic Programming", "dynamic-programming"));
        assertTrue(TitleMatcher.topicMatches("heap-priority-queue", "Heap (Priority Queue)"));
        assertTrue(TitleMatcher.topicMatches("Heap (Priority Queue)", "heap"));
    }

    @Test
    void normalizeCollapsesPunctuationAndCase() {
        assertEquals("heap priority queue", TitleMatcher.normalize("Heap (Priority Queue)"));
        assertEquals("two pointers", TitleMatcher.normalize("  Two--Pointers "));
        assertEquals("", TitleMatcher.normalize(null));
    }

    @Test
    void rejectsBlankAndNullInputs() {
        assertFalse(TitleMatcher.topicMatches(null, "arrays"));
        assertFalse(TitleMatcher.topicMatches("Arrays", null));
        assertFalse(TitleMatcher.topicMatches("   ", "arrays"));
        assertFalse(TitleMatcher.topicMatches("Arrays", "!!!"));
        assertFalse(TitleMatcher.topicMatches("Graph", "Trees"));
    }

    @Test
    void doesNotMatchUnrelatedConcepts() {
        assertFalse(TitleMatcher.topicMatches("Sorting", "sliding-window"));
    }
}
