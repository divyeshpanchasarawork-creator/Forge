package com.forge.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProblemLoaderTest {

    private static final String VALID_JSON = """
            {
              "arrays": [
                {"title": "Two Sum", "titleSlug": "two-sum", "difficulty": "Easy"},
                {"title": "Best Time to Buy and Sell Stock", "titleSlug": "best-time-to-buy-and-sell-stock", "difficulty": "Easy"}
              ],
              "hash-table": [
                {"title": "Two Sum", "titleSlug": "two-sum", "difficulty": "Easy"}
              ]
            }
            """;

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadsProblemsAndKeepsFirstTagForCrossTagSlugs() {
        ProblemLoader loader = new ProblemLoader();
        loader.load(stream(VALID_JSON));

        assertEquals(2, loader.getAllTagSlugs().size());
        assertEquals(2, loader.getProblemsForTag("arrays").size());
        assertEquals(1, loader.getProblemsForTag("hash-table").size());
        assertEquals("arrays", loader.getTagSlugForProblem("two-sum"));
        assertNull(loader.getTagSlugForProblem("unknown-slug"));
    }

    @Test
    void missingResourceFailsFast() {
        ProblemLoader loader = new ProblemLoader() {
            @Override
            InputStream openStream() {
                return null;
            }
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class, loader::init);

        assertTrue(ex.getMessage().contains("problems.json not found"));
    }

    @Test
    void corruptJsonFailsFastWithCause() {
        ProblemLoader loader = new ProblemLoader();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.load(stream("{ not valid json")));

        assertTrue(ex.getMessage().contains("Failed to parse problems.json"));
        assertNotNull(ex.getCause());
    }

    @Test
    void incompleteEntryFailsFastAndNamesTheTag() {
        ProblemLoader loader = new ProblemLoader();
        String json = """
                {
                  "dynamic-programming": [
                    {"title": "Climbing Stairs", "titleSlug": "climbing-stairs", "difficulty": ""},
                    {"title": "House Robber", "titleSlug": "house-robber", "difficulty": "Medium"}
                  ]
                }
                """;

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.load(stream(json)));

        assertTrue(ex.getMessage().contains("dynamic-programming"));
        assertTrue(ex.getMessage().contains("incomplete problem entry"));
    }

    @Test
    void missingFieldFailsFast() {
        ProblemLoader loader = new ProblemLoader();
        String json = """
                {
                  "stack": [
                    {"title": "Valid Parentheses", "difficulty": "Easy"}
                  ]
                }
                """;

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.load(stream(json)));

        assertTrue(ex.getMessage().contains("stack"));
    }

    @Test
    void realClasspathResourceLoadsCleanly() {
        ProblemLoader loader = new ProblemLoader();

        loader.init();

        assertFalse(loader.getAllProblems().isEmpty());
        assertTrue(loader.getAllTagSlugs().size() >= 20);
        for (ProblemLoader.ProblemEntry entry : loader.getAllProblems()) {
            assertNotNull(entry.getTitle());
            assertNotNull(entry.getTitleSlug());
            assertTrue(entry.getDifficulty().equals("Easy")
                    || entry.getDifficulty().equals("Medium")
                    || entry.getDifficulty().equals("Hard"),
                    "Unexpected difficulty: " + entry.getDifficulty());
        }
    }
}
