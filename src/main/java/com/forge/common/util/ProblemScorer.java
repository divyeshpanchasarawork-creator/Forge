package com.forge.common.util;

import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProblemScorer {

    private final LeetCodeTagStatRepository tagStatRepository;
    private final TopicRepository topicRepository;
    private final ProblemSuggestionRepository problemSuggestionRepository;

    public int score(UUID userId, ProblemLoader.ProblemEntry candidate, String tagSlug) {
        double total = 0;

        total += weakTagMatch(userId, tagSlug) * 0.30;
        total += difficultyFit(userId, candidate.getDifficulty()) * 0.20;
        total += topicConfidenceGap(userId, tagSlug) * 0.20;
        total += spacedRepetitionProximity(userId, tagSlug) * 0.15;
        total += notPreviouslySuggested(userId, candidate) * 0.10;
        total += diversityBonus(tagSlug) * 0.05;

        return (int) Math.round(Math.min(100, total));
    }

    private double weakTagMatch(UUID userId, String tagSlug) {
        return tagStatRepository.findByUserId(userId).stream()
                .filter(ts -> ts.getTagSlug().equals(tagSlug))
                .findFirst()
                .map(ts -> {
                    int solved = ts.getProblemsSolved() != null ? ts.getProblemsSolved() : 0;
                    if (solved == 0) return 100.0;
                    if (solved < 3) return 80.0;
                    if (solved < 5) return 50.0;
                    if (solved < 10) return 20.0;
                    return 0.0;
                })
                .orElse(70.0);
    }

    private double difficultyFit(UUID userId, String difficulty) {
        List<LeetCodeTagStat> stats = tagStatRepository.findByUserId(userId);
        if (stats.isEmpty()) return 50.0;
        int totalSolved = stats.stream()
                .mapToInt(ts -> ts.getProblemsSolved() != null ? ts.getProblemsSolved() : 0)
                .sum();
        int easy = totalSolved / 3;
        int hard = totalSolved / 10;
        double ratio = totalSolved > 0 ? (double) easy / totalSolved : 0.5;

        if ("EASY".equalsIgnoreCase(difficulty) && ratio > 0.4) return 30.0;
        if ("HARD".equalsIgnoreCase(difficulty) && hard < 5) return 80.0;
        if ("MEDIUM".equalsIgnoreCase(difficulty)) return 70.0;
        return 50.0;
    }

    private double topicConfidenceGap(UUID userId, String tagSlug) {
        List<Topic> topics = topicRepository.findByUserId(userId,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        String searchName = tagSlug.replace("-", " ").toLowerCase();
        return topics.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(searchName)
                        || searchName.contains(t.getTitle().toLowerCase()))
                .findFirst()
                .map(t -> {
                    int conf = t.getConfidence() != null ? t.getConfidence() : 5;
                    return Math.max(0, (10 - conf) * 10.0);
                })
                .orElse(50.0);
    }

    private double spacedRepetitionProximity(UUID userId, String tagSlug) {
        List<Topic> topics = topicRepository.findByUserId(userId,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        String searchName = tagSlug.replace("-", " ").toLowerCase();
        return topics.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(searchName)
                        || searchName.contains(t.getTitle().toLowerCase()))
                .findFirst()
                .map(t -> {
                    if (t.getNextRevision() == null) return 30.0;
                    long daysUntil = Duration.between(LocalDateTime.now(), t.getNextRevision()).toDays();
                    if (daysUntil <= 0) return 100.0;
                    if (daysUntil <= 3) return 80.0;
                    if (daysUntil <= 7) return 50.0;
                    return 10.0;
                })
                .orElse(30.0);
    }

    private double notPreviouslySuggested(UUID userId, ProblemLoader.ProblemEntry candidate) {
        boolean suggestedFromWeakTag = problemSuggestionRepository.findByUserId(userId).stream()
                .anyMatch(ps -> ps.getTitleSlug().equals(candidate.getTitleSlug())
                        && "WEAK_TAG".equals(ps.getSource()));
        if (suggestedFromWeakTag) return 0.0;
        return 100.0;
    }

    private double diversityBonus(String tagSlug) {
        return 50.0;
    }

    public record ScoredProblem(ProblemLoader.ProblemEntry problem, String tagSlug, int score) {}
}
