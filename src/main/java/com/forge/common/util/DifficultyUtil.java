package com.forge.common.util;

import java.util.Locale;
import java.util.Map;

public final class DifficultyUtil {

    private static final Map<String, String> CANONICAL = Map.of(
            "EASY", "Easy",
            "MEDIUM", "Medium",
            "HARD", "Hard"
    );

    private DifficultyUtil() {
    }

    /**
     * Normalizes LeetCode-sourced difficulty (uppercase from the GraphQL API,
     * title-case from the curated problem set) to a single canonical form.
     */
    public static String titleCase(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return difficulty;
        }
        return CANONICAL.getOrDefault(difficulty.toUpperCase(Locale.ROOT), difficulty);
    }
}
