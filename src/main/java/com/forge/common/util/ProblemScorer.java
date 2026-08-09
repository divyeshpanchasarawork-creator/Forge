package com.forge.common.util;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.calibration.service.ScorerWeightsService;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ScorerWeightsService scorerWeightsService;

    private record Signal(String name, double weight, double value) {}

    public record ScoreItem(String name, double weight, int value, int contribution) {}

    public record ScoreBreakdown(int total, List<ScoreItem> items) {}

    public record ScoringContext(List<LeetCodeTagStat> stats, List<Topic> topics,
                                 List<ProblemAttempt> attempts, List<ProblemSuggestion> suggestions,
                                 int targetLevel, RewardModel.RewardStats rewards, SignalWeights weights,
                                 double userSkill, int totalSolved,
                                 Map<String, LocalDateTime> lastAttemptBySlug,
                                 Map<String, Long> recentWeekTagCounts,
                                 Map<String, Long> recentTagCounts, int recentSize,
                                 ZoneId zone) {

        public ScoringContext(List<LeetCodeTagStat> stats, List<Topic> topics,
                              List<ProblemAttempt> attempts, List<ProblemSuggestion> suggestions,
                              int targetLevel, RewardModel.RewardStats rewards, SignalWeights weights,
                              ZoneId zone) {
            this(stats, topics, attempts, suggestions, targetLevel, rewards, weights,
                    SkillRatingService.skillFromTopics(topics),
                    ProblemScorer.totalSolved(stats),
                    ProblemScorer.lastAttemptBySlug(attempts),
                    ProblemScorer.recentWeekTagCounts(attempts, zone),
                    ProblemScorer.recentTagCounts(attempts),
                    ProblemScorer.recentSize(attempts),
                    zone);
        }

        public List<String> suggestedSlugs() {
            return suggestions.stream().map(ProblemSuggestion::getTitleSlug).toList();
        }
    }

    public ScoringContext context(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        ZoneId zone = TimezoneUtil.resolve(user);
        List<LeetCodeTagStat> stats = tagStatRepository.findByUserId(userId);
        List<Topic> topics = topicRepository.findByUserId(userId, PageRequest.of(0, 100));
        List<ProblemAttempt> attempts = problemAttemptRepository
                .findByUserIdOrderByAttemptedAtDesc(userId, PageRequest.of(0, 500));
        List<ProblemSuggestion> suggestions = problemSuggestionRepository.findByUserId(userId);
        int targetLevel = user != null && user.getTargetLevel() != null ? user.getTargetLevel() : 5;
        return new ScoringContext(stats, topics, attempts, suggestions, targetLevel,
                RewardModel.stats(attempts), scorerWeightsService.currentWeights(), zone);
    }

    public ScoreBreakdown breakdown(ScoringContext ctx, ProblemLoader.ProblemEntry candidate, String tagSlug) {
        double[] w = ctx.weights().toArray();
        List<Signal> signals = List.of(
                new Signal(SignalWeights.SIGNAL_NAMES.get(0), w[0], weakTagMatch(ctx.stats(), tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(1), w[1], topicMasteryGap(ctx.topics(), tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(2), w[2], difficultyFit(ctx, candidate.getDifficulty())),
                new Signal(SignalWeights.SIGNAL_NAMES.get(3), w[3], expectedLearningGain(ctx, tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(4), w[4], revisionUrgency(ctx, tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(5), w[5], confidenceDecay(ctx, tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(6), w[6], readiness(ctx, candidate.getDifficulty())),
                new Signal(SignalWeights.SIGNAL_NAMES.get(7), w[7], timeSinceLastPractice(ctx, candidate.getTitleSlug())),
                new Signal(SignalWeights.SIGNAL_NAMES.get(8), w[8], coverageBalance(ctx, tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(9), w[9], goalAlignment(candidate.getDifficulty(), ctx.targetLevel())),
                new Signal(SignalWeights.SIGNAL_NAMES.get(10), w[10], notPreviouslySuggested(ctx.suggestedSlugs(), candidate)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(11), w[11], diversity(ctx, tagSlug)),
                new Signal(SignalWeights.SIGNAL_NAMES.get(12), w[12], ucbExploration(ctx.rewards(), candidate.getTitleSlug()))
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

    private double difficultyFit(ScoringContext ctx, String difficulty) {
        if (ctx.stats().isEmpty()) return 50.0;
        int totalSolved = ctx.totalSolved();
        if (totalSolved == 0) return 60.0;
        int easy = totalSolved / 3;
        int hard = totalSolved / 10;
        double ratio = (double) easy / totalSolved;

        if ("EASY".equalsIgnoreCase(difficulty) && ratio > 0.4) return 30.0;
        if ("HARD".equalsIgnoreCase(difficulty) && hard < 5) return 80.0;
        if ("MEDIUM".equalsIgnoreCase(difficulty)) return 70.0;
        return 50.0;
    }

    private double expectedLearningGain(ScoringContext ctx, String tagSlug) {
        return ctx.topics().stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    int mastery = t.getMastery() != null ? t.getMastery() : 0;
                    boolean fresh = t.getLastAttemptAt() != null
                            && Duration.between(t.getLastAttemptAt(), LocalDateTime.now(ctx.zone())).toDays() < 3;
                    return Math.max(0, (100 - mastery) * 0.6) + (fresh ? 10 : 40);
                })
                .orElse(70.0);
    }

    private double revisionUrgency(ScoringContext ctx, String tagSlug) {
        return ctx.topics().stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    if (t.getNextRevision() != null && !t.getNextRevision().isAfter(LocalDateTime.now(ctx.zone()))) return 100.0;
                    double retention = t.getEstimatedRetention() != null ? t.getEstimatedRetention() : 100;
                    if (retention <= 50) return 100.0;
                    if (retention <= 70) return 75.0;
                    if (retention <= 85) return 50.0;
                    return 20.0;
                })
                .orElse(30.0);
    }

    private double confidenceDecay(ScoringContext ctx, String tagSlug) {
        return ctx.topics().stream()
                .filter(t -> matches(t.getTitle(), tagSlug))
                .findFirst()
                .map(t -> {
                    LocalDateTime anchor = t.getLastAttemptAt() != null ? t.getLastAttemptAt() : t.getLastRevision();
                    if (anchor == null) return 50.0;
                    long days = Duration.between(anchor, LocalDateTime.now(ctx.zone())).toDays();
                    if (days >= 14) return 100.0;
                    if (days >= 7) return 70.0;
                    if (days >= 3) return 40.0;
                    return 10.0;
                })
                .orElse(30.0);
    }

    private double readiness(ScoringContext ctx, String difficulty) {
        double skill = ctx.userSkill();
        double difficultyRating = skillRatingService.difficultyRating(difficulty);
        double diff = Math.abs(skill - difficultyRating);
        return Math.max(0, 100 - diff / 8);
    }

    private double timeSinceLastPractice(ScoringContext ctx, String problemSlug) {
        LocalDateTime last = ctx.lastAttemptBySlug().get(problemSlug);
        if (last == null) return 100.0;
        long days = Duration.between(last, LocalDateTime.now(ctx.zone())).toDays();
        if (days >= 7) return 100.0;
        if (days >= 3) return 70.0;
        if (days >= 1) return 40.0;
        return 10.0;
    }

    private double coverageBalance(ScoringContext ctx, String tagSlug) {
        if (tagSlug == null) return 50.0;
        long recent = ctx.recentWeekTagCounts().getOrDefault(tagSlug, 0L);
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

    private double diversity(ScoringContext ctx, String tagSlug) {
        if (tagSlug == null) return 50.0;
        if (ctx.recentSize() == 0) return 100.0;
        long sameTag = ctx.recentTagCounts().getOrDefault(tagSlug, 0L);
        return 100.0 * (1.0 - sameTag / (double) ctx.recentSize());
    }

    private double ucbExploration(RewardModel.RewardStats rewards, String problemSlug) {
        if (rewards == null || rewards.totalCount() == 0) return 50.0;
        RewardModel.Reward r = rewards.byProblem().get(problemSlug);
        double exploitation = r != null ? r.mean() * 100.0 : 0.0;
        double exploration = 25.0 * Math.sqrt(Math.log(rewards.totalCount() + 1) / (r != null ? r.count() + 1 : 1));
        return Math.min(100, Math.max(0, exploitation + exploration));
    }

    private boolean matches(String topicTitle, String tagSlug) {
        if (topicTitle == null || tagSlug == null) return false;
        String searchName = tagSlug.replace("-", " ").toLowerCase();
        String title = topicTitle.toLowerCase();
        return title.contains(searchName) || searchName.contains(title);
    }

    private static int totalSolved(List<LeetCodeTagStat> stats) {
        return stats.stream()
                .mapToInt(ts -> ts.getProblemsSolved() != null ? ts.getProblemsSolved() : 0)
                .sum();
    }

    private static Map<String, LocalDateTime> lastAttemptBySlug(List<ProblemAttempt> attempts) {
        Map<String, LocalDateTime> last = new HashMap<>();
        for (ProblemAttempt a : attempts) {
            if (a.getAttemptedAt() == null) continue;
            LocalDateTime existing = last.get(a.getProblemSlug());
            if (existing == null || a.getAttemptedAt().isAfter(existing)) {
                last.put(a.getProblemSlug(), a.getAttemptedAt());
            }
        }
        return last;
    }

    private static Map<String, Long> recentWeekTagCounts(List<ProblemAttempt> attempts, ZoneId zone) {
        LocalDate weekAgo = LocalDate.now(zone).minusDays(7);
        Map<String, Long> counts = new HashMap<>();
        for (ProblemAttempt a : attempts) {
            if (a.getAttemptedAt() == null || a.getTopicTagSlug() == null) continue;
            if (a.getAttemptedAt().toLocalDate().isBefore(weekAgo)) continue;
            counts.merge(a.getTopicTagSlug(), 1L, Long::sum);
        }
        return counts;
    }

    private static Map<String, Long> recentTagCounts(List<ProblemAttempt> attempts) {
        List<ProblemAttempt> recent = attempts.stream()
                .filter(a -> a.getAttemptedAt() != null)
                .sorted(Comparator.comparing(ProblemAttempt::getAttemptedAt).reversed())
                .limit(30)
                .toList();
        Map<String, Long> counts = new HashMap<>();
        for (ProblemAttempt a : recent) {
            if (a.getTopicTagSlug() != null) {
                counts.merge(a.getTopicTagSlug(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private static int recentSize(List<ProblemAttempt> attempts) {
        return Math.min(30, (int) attempts.stream().filter(a -> a.getAttemptedAt() != null).count());
    }
}
