package com.forge.intelligence.service;

import com.forge.topic.entity.Topic;
import org.springframework.stereotype.Service;

@Service
public class SkillRatingService {

    public static final double INITIAL_RATING = 1000.0;
    private static final double MIN_RATING = 400.0;
    private static final double MAX_RATING = 2800.0;

    public double difficultyRating(String difficulty) {
        if (difficulty == null) return 1200.0;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 800.0;
            case "MEDIUM" -> 1200.0;
            case "HARD" -> 1600.0;
            default -> 1200.0;
        };
    }

    public double expectedScore(double rating, double opponentRating) {
        return 1.0 / (1.0 + Math.pow(10, (opponentRating - rating) / 400.0));
    }

    public double kFactor(int attemptsTotal) {
        double base = attemptsTotal < 10 ? 48.0 : (attemptsTotal < 30 ? 36.0 : 24.0);
        return base;
    }

    public double applyResult(double rating, String difficulty, boolean solved, int attemptsTotal) {
        double difficultyRating = difficultyRating(difficulty);
        double expected = expectedScore(rating, difficultyRating);
        double actual = solved ? 1.0 : 0.0;
        double k = kFactor(attemptsTotal);
        double updated = rating + k * (actual - expected);
        return Math.max(MIN_RATING, Math.min(MAX_RATING, updated));
    }

    public double userSkillFromTopics(java.util.List<Topic> topics) {
        return skillFromTopics(topics);
    }

    public static double skillFromTopics(java.util.List<Topic> topics) {
        if (topics == null || topics.isEmpty()) return INITIAL_RATING;
        long totalAttempts = topics.stream()
                .mapToLong(t -> t.getAttemptsTotal() != null ? t.getAttemptsTotal() : 0)
                .sum();
        if (totalAttempts == 0) return INITIAL_RATING;
        return topics.stream()
                .filter(t -> t.getSkillRating() != null)
                .mapToDouble(t -> t.getSkillRating() * (t.getAttemptsTotal() != null ? t.getAttemptsTotal() : 0))
                .sum() / totalAttempts;
    }
}
