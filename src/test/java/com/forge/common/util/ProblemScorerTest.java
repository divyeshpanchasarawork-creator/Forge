package com.forge.common.util;

import com.forge.calibration.service.ScorerWeightsService;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.recommendation.repository.RecommendationRepository;
import com.forge.topic.repository.TopicRepository;
import com.forge.auth.repository.UserRepository;
import com.forge.intelligence.service.SkillRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProblemScorerTest {

    @Mock private LeetCodeTagStatRepository tagStatRepository;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ProblemSuggestionRepository problemSuggestionRepository;
    @Mock private ProblemAttemptRepository problemAttemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillRatingService skillRatingService;
    @Mock private ScorerWeightsService scorerWeightsService;
    @Mock private RecommendationRepository recommendationRepository;

    private ProblemScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ProblemScorer(tagStatRepository, snapshotRepository, topicRepository,
                problemSuggestionRepository, problemAttemptRepository, userRepository, skillRatingService,
                scorerWeightsService, recommendationRepository);
    }

    private ProblemScorer.ScoringContext ctx(List<ProblemAttempt> attempts) {
        return new ProblemScorer.ScoringContext(List.of(), List.of(), attempts, List.of(), 5,
                RewardModel.stats(attempts), SignalWeights.DEFAULT, java.util.Map.of(), java.time.ZoneId.of("UTC"));
    }

    private ProblemScorer.ScoringContext ctxWithDifficulty(ProblemScorer.DifficultyStats difficultyStats) {
        LeetCodeTagStat stat = new LeetCodeTagStat();
        stat.setTagSlug("arrays");
        return new ProblemScorer.ScoringContext(List.of(stat), List.of(), List.of(), List.of(), 5,
                RewardModel.stats(List.of()), SignalWeights.DEFAULT, java.util.Map.of(),
                0.0, 0, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), 0,
                java.time.ZoneId.of("UTC"), difficultyStats);
    }

    private double difficultyFitValue(ProblemScorer.ScoreBreakdown breakdown) {
        return breakdown.items().stream()
                .filter(item -> item.name().equals(SignalWeights.SIGNAL_NAMES.get(2)))
                .findFirst().orElseThrow().value();
    }

    @Test
    void difficultyFitShouldDecayEasyUsingRealEasyCount() {
        double fresh = difficultyFitValue(scorer.breakdown(ctxWithDifficulty(ProblemScorer.DifficultyStats.NONE),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "EASY"), "arrays"));
        double seasoned = difficultyFitValue(scorer.breakdown(
                ctxWithDifficulty(new ProblemScorer.DifficultyStats(20, 10, 2)),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "EASY"), "arrays"));

        assertEquals(60.0, fresh);
        assertEquals(30.0, seasoned);
    }

    @Test
    void difficultyFitShouldBoostHardUntilFiveRealHardSolves() {
        ProblemLoader.ProblemEntry hard = new ProblemLoader.ProblemEntry("Hard Problem", "hard-problem", "HARD");

        double fewHards = difficultyFitValue(scorer.breakdown(
                ctxWithDifficulty(new ProblemScorer.DifficultyStats(10, 8, 4)), hard, "arrays"));
        double manyHards = difficultyFitValue(scorer.breakdown(
                ctxWithDifficulty(new ProblemScorer.DifficultyStats(10, 8, 6)), hard, "arrays"));

        assertEquals(80.0, fewHards);
        assertEquals(56.0, manyHards);
    }

    @Test
    void ucbExplorationShouldRewardUnseenProblems() {
        List<ProblemAttempt> attempts = new java.util.ArrayList<>();
        IntStream.range(0, 15).forEach(i -> {
            ProblemAttempt a = new ProblemAttempt();
            a.setProblemSlug("two-sum");
            a.setAttemptedAt(java.time.LocalDateTime.now());
            attempts.add(a);
        });

        ProblemScorer.ScoreBreakdown unseen = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Unseen", "unseen", "Medium"), "arrays");
        ProblemScorer.ScoreBreakdown practiced = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Medium"), "arrays");

        double unseenUcb = signal(unseen, "UCB exploration");
        double practicedUcb = signal(practiced, "UCB exploration");

        assertTrue(unseenUcb > practicedUcb,
                "Unseen problem should get a higher UCB exploration bonus, got " + unseenUcb + " vs " + practicedUcb);
    }

    @Test
    void ucbExplorationShouldBeNeutralWhenNoAttemptHistory() {
        ProblemScorer.ScoreBreakdown breakdown = scorer.breakdown(ctx(List.of()),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Medium"), "arrays");

        assertEquals(50.0, signal(breakdown, "UCB exploration"));
    }

    @Test
    void ucbExplorationShouldExploitHighRewardProblems() {
        List<ProblemAttempt> attempts = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ProblemAttempt good = new ProblemAttempt();
            good.setProblemSlug("two-sum");
            good.setQuality(5);
            good.setAttemptedAt(java.time.LocalDateTime.now());
            attempts.add(good);

            ProblemAttempt bad = new ProblemAttempt();
            bad.setProblemSlug("single-number");
            bad.setQuality(0);
            bad.setAttemptedAt(java.time.LocalDateTime.now());
            attempts.add(bad);
        }

        ProblemScorer.ScoreBreakdown good = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Medium"), "arrays");
        ProblemScorer.ScoreBreakdown bad = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Single Number", "single-number", "Medium"), "arrays");

        assertTrue(signal(good, "UCB exploration") > signal(bad, "UCB exploration"),
                "A high-reward problem should beat a low-reward one under reward-aware UCB, got "
                        + signal(good, "UCB exploration") + " vs " + signal(bad, "UCB exploration"));
    }

    @Test
    void totalShouldNotExceedHundred() {
        ProblemScorer.ScoreBreakdown breakdown = scorer.breakdown(ctx(List.of()),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy"), "arrays");

        assertTrue(breakdown.total() <= 100);
    }

    @Test
    void diversityShouldVaryWithRecentTagCoverage() {
        List<ProblemAttempt> attempts = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            ProblemAttempt a = new ProblemAttempt();
            a.setProblemSlug("two-sum-" + i);
            a.setTopicTagSlug("arrays");
            a.setAttemptedAt(java.time.LocalDateTime.now().minusMinutes(i));
            attempts.add(a);
        }

        ProblemScorer.ScoreBreakdown overCovered = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Medium"), "arrays");
        ProblemScorer.ScoreBreakdown fresh = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Medium"), "strings");

        double arraysDiversity = signal(overCovered, "Diversity");
        double stringsDiversity = signal(fresh, "Diversity");

        assertTrue(stringsDiversity > arraysDiversity,
                "A tag absent from recent practice should score higher diversity than an over-covered one, got "
                        + stringsDiversity + " vs " + arraysDiversity);
        assertEquals(0.0, arraysDiversity);
        assertEquals(100.0, stringsDiversity);
    }

    @Test
    void diversityShouldBeMaximalWhenNoAttemptHistory() {
        ProblemScorer.ScoreBreakdown breakdown = scorer.breakdown(ctx(List.of()),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Medium"), "arrays");

        assertEquals(100.0, signal(breakdown, "Diversity"));
    }

    @Test
    void ucbExplorationShouldBlendTagLevelReward() {
        List<ProblemAttempt> attempts = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ProblemAttempt a = new ProblemAttempt();
            a.setProblemSlug("other-" + i);
            a.setTopicTagSlug("arrays");
            a.setQuality(5);
            a.setAttemptedAt(java.time.LocalDateTime.now().minusHours(i));
            attempts.add(a);
        }

        ProblemScorer.ScoreBreakdown inStrongTag = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Fresh A", "fresh-a", "Medium"), "arrays");
        ProblemScorer.ScoreBreakdown inUnknownTag = scorer.breakdown(ctx(attempts),
                new ProblemLoader.ProblemEntry("Fresh B", "fresh-b", "Medium"), "tag-with-no-history");

        assertTrue(signal(inStrongTag, "UCB exploration") > signal(inUnknownTag, "UCB exploration"),
                "An untried problem in a well-rewarded tag should out-pull one in an unrewarded tag, got "
                        + signal(inStrongTag, "UCB exploration") + " vs " + signal(inUnknownTag, "UCB exploration"));
    }

    @Test
    void notSuggestedShouldSuppressRecentlyDismissedProblemsOnly() {
        java.time.ZoneId zone = java.time.ZoneId.of("UTC");
        java.util.Map<String, java.time.LocalDateTime> dismissed = java.util.Map.of(
                "dismissed-yesterday", java.time.LocalDateTime.now(zone).minusDays(1),
                "dismissed-weeks-ago", java.time.LocalDateTime.now(zone).minusDays(14));
        ProblemScorer.ScoringContext c = new ProblemScorer.ScoringContext(List.of(), List.of(),
                List.of(), List.of(), 5, RewardModel.stats(List.of()), SignalWeights.DEFAULT,
                dismissed, zone);

        double recentDismissal = signal(scorer.breakdown(c,
                new ProblemLoader.ProblemEntry("Recent", "dismissed-yesterday", "Medium"), "arrays"), "Not suggested");
        double oldDismissal = signal(scorer.breakdown(c,
                new ProblemLoader.ProblemEntry("Old", "dismissed-weeks-ago", "Medium"), "arrays"), "Not suggested");
        double neverDismissed = signal(scorer.breakdown(c,
                new ProblemLoader.ProblemEntry("New", "never-dismissed", "Medium"), "arrays"), "Not suggested");

        assertEquals(0.0, recentDismissal, "recently dismissed problems must be suppressed");
        assertEquals(100.0, oldDismissal, "old dismissals expire so problems can resurface");
        assertEquals(100.0, neverDismissed);
    }

    private double signal(ProblemScorer.ScoreBreakdown breakdown, String name) {
        return breakdown.items().stream()
                .filter(i -> i.name().equals(name))
                .findFirst()
                .map(ProblemScorer.ScoreItem::value)
                .orElseThrow(() -> new AssertionError("Signal not found: " + name));
    }
}
