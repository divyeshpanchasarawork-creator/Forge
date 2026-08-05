package com.forge.common.util;

import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
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
    @Mock private TopicRepository topicRepository;
    @Mock private ProblemSuggestionRepository problemSuggestionRepository;
    @Mock private ProblemAttemptRepository problemAttemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillRatingService skillRatingService;

    private ProblemScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ProblemScorer(tagStatRepository, topicRepository, problemSuggestionRepository,
                problemAttemptRepository, userRepository, skillRatingService);
    }

    private ProblemScorer.ScoringContext ctx(List<ProblemAttempt> attempts) {
        return new ProblemScorer.ScoringContext(null, List.of(), List.of(), attempts, List.of(), 5);
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
    void totalShouldNotExceedHundred() {
        ProblemScorer.ScoreBreakdown breakdown = scorer.breakdown(ctx(List.of()),
                new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy"), "arrays");

        assertTrue(breakdown.total() <= 100);
    }

    private double signal(ProblemScorer.ScoreBreakdown breakdown, String name) {
        return breakdown.items().stream()
                .filter(i -> i.name().equals(name))
                .findFirst()
                .map(ProblemScorer.ScoreItem::value)
                .orElseThrow(() -> new AssertionError("Signal not found: " + name));
    }
}
