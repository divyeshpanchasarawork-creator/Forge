package com.forge.common.util;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import com.forge.intelligence.service.SkillRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProblemScorer {

    private final LeetCodeTagStatRepository tagStatRepository;
    private final TopicRepository topicRepository;
    private final ProblemSuggestionRepository problemSuggestionRepository;
    private final ProblemAttemptRepository problemAttemptRepository;
    private final UserRepository userRepository;
    private final SkillRatingService skillRatingService;

    private record Signal(String name, double weight, double value) {}

    public record ScoreItem(String name, double weight, int value, int contribution) {}

    public record ScoreBreakdown(int total, List<ScoreItem> items) {}

    public record ScoredProblem(ProblemLoader.ProblemEntry problem, String tagSlug, int score, ScoreBreakdown breakdown) {}

    public record ScoringContext(User user, List<LeetCodeTagStat> stats, List<Topic> topics,
                                 List<ProblemAttempt> attempts, List<String> suggestedSlugs, int targetLevel) {}

    public ScoringContext context(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        List<LeetCodeTagStat> stats = tagStatRepository.findByUserId(userId);
        List<Topic> topics = topicRepository.findByUserId(userId, PageRequest.of(0, 100)).getContent();
        List<ProblemAttempt> attempts = problemAttemptRepository.findByUserIdAll(userId);
        List<String> suggestedSlugs = problemSuggestionRepository.findByUserId(userId).stream()
                .map(com.forge.leetcode.entity.ProblemSuggestion::getTitleSlug)
                .toList();
        int targetLevel = user != null && user.getTargetLevel() != null ? user.getTargetLevel() : 5;
        return new ScoringContext(user, stats, topics, attempts, suggestedSlugs, targetLevel);
    }

    public ScoreBreakdown breakdown(ScoringContext ctx, ProblemLoader.ProblemEntry candidate, String tagSlug) {
        List<Signal> signals = List.of(
                new Signal("Weak tag", 0.15, weakTagMatch(ctx.stats(), tagSlug)),
                new Signal("Mastery gap", 0.12, topicMasteryGap(ctx.topics(), tagSlug)),
                new Signal("Difficulty fit", 0.10, difficultyFit(ctx.stats(), candidate.getDifficulty())),
                new Signal("Learning gain", 0.10, expectedLearningGain(ctx.topics(), tagSlug)),
                new Signal("Revision urgency", 0.10, revisionUrgency(ctx.topics(), tagSlug)),
                new Signal("Confidence decay", 0.08, confidenceDecay(ctx.topics(), tagSlug)),
                new Signal("Readiness", 0.08, readiness(ctx.topics(), candidate.getDifficulty())),
                new Signal("Time since practice", 0.08, timeSinceLastPractice(ctx.attempts(), candidate.getTitleSlug(), tagSlug)),
                new Signal("Coverage balance", 0.07, coverageBalance(ctx.attempts(), tagSlug)),
                new Signal("Goal alignment", 0.06, goalAlignment(candidate.getDifficulty(), ctx.targetLevel())),
                new Signal("Not suggested", 0.04, notPreviouslySuggested(ctx.suggestedSlugs(), candidate)),
                new Signal("Diversity", 0.02, 50.0)
        );

        double total = 0;
        List<ScoreItem> items = new ArrayList<>();
        for (Signal signal : signals) {
            double contribution = signal.value() * signal.weight();
            total += contribution;
            items.add(new ScoreItem(signal.name(), signal.weight(), (int) Math.round(signal.value()), (int) Math.round(contribution)));
        }

        return new ScoreBreakdown(Math.min(100, (int) Math.round(total)), items);
    }

    public int score(ScoringContext ctx, ProblemLoader.ProblemEntry candidate, String tagSlug) {
        return breakdown(ctx, candidate, tagSlug).total();
    }

    public ScoreBreakdown breakdown(UUID userId, ProblemLoader.ProblemEntry candidate, String tagSlug) {
        return breakdown(context(userId), candidate, tagSlug);
    }

    public int score(UUID userId, ProblemLoader.ProblemEntry candidate, String tagSlug) {
        return score(context(userId), candidate, tagSlug);
    }

    private double weakTagMatch(List<LeetCodeTagStat> stats, String tagSlug) {
        return stats.stream()
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

    private double topicMasteryGap(List<Topic> topics, String tagSlug) {
        return topics.stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    int conf = t.getConfidence() != null ? t.getConfidence() : 5;
                    int mastery = t.getMastery() != null ? t.getMastery() : 0;
                    return Math.max(0, (10 - conf) * 10) * 0.6 + (100 - mastery) * 0.4;
                })
                .orElse(50.0);
    }

    private double difficultyFit(List<LeetCodeTagStat> stats, String difficulty) {
        if (stats.isEmpty()) return 50.0;
        int totalSolved = stats.stream()
                .mapToInt(ts -> ts.getProblemsSolved() != null ? ts.getProblemsSolved() : 0)
                .sum();
        if (totalSolved == 0) return 60.0;
        int easy = totalSolved / 3;
        int hard = totalSolved / 10;
        double ratio = (double) easy / totalSolved;

        if ("EASY".equalsIgnoreCase(difficulty) && ratio > 0.4) return 30.0;
        if ("HARD".equalsIgnoreCase(difficulty) && hard < 5) return 80.0;
        if ("MEDIUM".equalsIgnoreCase(difficulty)) return 70.0;
        return 50.0;
    }

    private double expectedLearningGain(List<Topic> topics, String tagSlug) {
        return topics.stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    int mastery = t.getMastery() != null ? t.getMastery() : 0;
                    boolean fresh = t.getLastAttemptAt() != null
                            && Duration.between(t.getLastAttemptAt(), LocalDateTime.now()).toDays() < 3;
                    return Math.max(0, (100 - mastery) * 0.6) + (fresh ? 10 : 40);
                })
                .orElse(70.0);
    }

    private double revisionUrgency(List<Topic> topics, String tagSlug) {
        return topics.stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    if (t.getNextRevision() != null && !t.getNextRevision().isAfter(LocalDateTime.now())) return 100.0;
                    double retention = t.getEstimatedRetention() != null ? t.getEstimatedRetention() : 100;
                    if (retention <= 50) return 100.0;
                    if (retention <= 70) return 75.0;
                    if (retention <= 85) return 50.0;
                    return 20.0;
                })
                .orElse(30.0);
    }

    private double confidenceDecay(List<Topic> topics, String tagSlug) {
        return topics.stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    LocalDateTime anchor = t.getLastAttemptAt() != null ? t.getLastAttemptAt() : t.getLastRevision();
                    if (anchor == null) return 50.0;
                    long days = Duration.between(anchor, LocalDateTime.now()).toDays();
                    if (days >= 14) return 100.0;
                    if (days >= 7) return 70.0;
                    if (days >= 3) return 40.0;
                    return 10.0;
                })
                .orElse(30.0);
    }

    private double readiness(List<Topic> topics, String difficulty) {
        double skill = skillRatingService.userSkillFromTopics(topics);
        double difficultyRating = skillRatingService.difficultyRating(difficulty);
        double diff = Math.abs(skill - difficultyRating);
        return Math.max(0, 100 - diff / 8);
    }

    private double timeSinceLastPractice(List<ProblemAttempt> attempts, String problemSlug, String tagSlug) {
        LocalDateTime last = attempts.stream()
                .filter(a -> a.getProblemSlug().equals(problemSlug))
                .map(ProblemAttempt::getAttemptedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (last == null) return 100.0;
        long days = Duration.between(last, LocalDateTime.now()).toDays();
        if (days >= 7) return 100.0;
        if (days >= 3) return 70.0;
        if (days >= 1) return 40.0;
        return 10.0;
    }

    private double coverageBalance(List<ProblemAttempt> attempts, String tagSlug) {
        if (tagSlug == null) return 50.0;
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        long recent = attempts.stream()
                .filter(a -> tagSlug.equals(a.getTopicTagSlug()))
                .filter(a -> a.getAttemptedAt() != null && !a.getAttemptedAt().toLocalDate().isBefore(weekAgo))
                .count();
        if (recent == 0) return 100.0;
        if (recent == 1) return 60.0;
        if (recent == 2) return 30.0;
        return 10.0;
    }

    private double goalAlignment(String difficulty, int targetLevel) {
        String band = targetLevel <= 3 ? "EASY" : (targetLevel <= 6 ? "MEDIUM" : "HARD");
        String upper = targetLevel <= 3 ? "MEDIUM" : "HARD";
        String lower = targetLevel <= 6 ? "EASY" : "MEDIUM";
        if (band.equalsIgnoreCase(difficulty)) return 100.0;
        if (upper.equalsIgnoreCase(difficulty) || lower.equalsIgnoreCase(difficulty)) return 50.0;
        return 20.0;
    }

    private double notPreviouslySuggested(List<String> suggestedSlugs, ProblemLoader.ProblemEntry candidate) {
        if (suggestedSlugs.contains(candidate.getTitleSlug())) return 0.0;
        return 100.0;
    }

    private boolean matches(String topicTitle, String tagSlug) {
        if (topicTitle == null || tagSlug == null) return false;
        String searchName = tagSlug.replace("-", " ").toLowerCase();
        String title = topicTitle.toLowerCase();
        return title.contains(searchName) || searchName.contains(title);
    }
}
