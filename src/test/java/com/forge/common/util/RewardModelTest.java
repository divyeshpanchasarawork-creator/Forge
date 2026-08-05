package com.forge.common.util;

import com.forge.practice.entity.ProblemAttempt;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RewardModelTest {

    private ProblemAttempt attempt(int quality, String slug, String tag) {
        ProblemAttempt a = new ProblemAttempt();
        a.setProblemSlug(slug);
        a.setTopicTagSlug(tag);
        a.setQuality(quality);
        a.setAttemptedAt(LocalDateTime.now());
        return a;
    }

    @Test
    void rewardShouldScaleQualityToZeroToOne() {
        assertEquals(1.0, RewardModel.reward(attempt(5, "a", "arrays")));
        assertEquals(0.6, RewardModel.reward(attempt(3, "a", "arrays")));
        assertEquals(0.0, RewardModel.reward(attempt(0, "a", "arrays")));
    }

    @Test
    void rewardShouldHandleNullQuality() {
        ProblemAttempt a = new ProblemAttempt();
        a.setProblemSlug("a");
        a.setAttemptedAt(LocalDateTime.now());
        assertEquals(0.0, RewardModel.reward(a));
    }

    @Test
    void statsShouldAggregateByProblemAndTag() {
        List<ProblemAttempt> attempts = List.of(
                attempt(5, "two-sum", "arrays"),
                attempt(3, "two-sum", "arrays"),
                attempt(1, "single-number", "arrays"),
                attempt(4, "valid-anagram", "strings")
        );

        RewardModel.RewardStats stats = RewardModel.stats(attempts);

        assertEquals(4, stats.totalCount());
        RewardModel.Reward twoSum = stats.byProblem().get("two-sum");
        assertEquals(2, twoSum.count());
        assertEquals(0.8, twoSum.mean(), 1e-9);
        RewardModel.Reward arrays = stats.byTag().get("arrays");
        assertEquals(3, arrays.count());
        assertEquals((1.0 + 0.6 + 0.2) / 3, arrays.mean(), 1e-9);
    }

    @Test
    void statsShouldIgnoreMissingTag() {
        RewardModel.RewardStats stats = RewardModel.stats(List.of(attempt(5, "two-sum", null)));
        assertEquals(1, stats.byProblem().get("two-sum").count());
        assertTrue(stats.byTag().isEmpty());
    }
}
