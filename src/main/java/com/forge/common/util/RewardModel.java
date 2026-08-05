package com.forge.common.util;

import com.forge.practice.entity.ProblemAttempt;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class RewardModel {

    public record Reward(double mean, int count) {}

    public record RewardStats(int totalCount, Map<String, Reward> byProblem, Map<String, Reward> byTag) {}

    public static double reward(ProblemAttempt attempt) {
        int quality = attempt.getQuality() != null ? attempt.getQuality() : 0;
        return quality / 5.0;
    }

    public static RewardStats stats(List<ProblemAttempt> attempts) {
        Map<String, Reward> byProblem = new HashMap<>();
        Map<String, Reward> byTag = new HashMap<>();
        for (ProblemAttempt attempt : attempts) {
            double r = reward(attempt);
            accumulate(byProblem, attempt.getProblemSlug(), r);
            if (attempt.getTopicTagSlug() != null) {
                accumulate(byTag, attempt.getTopicTagSlug(), r);
            }
        }
        return new RewardStats(attempts.size(), byProblem, byTag);
    }

    private static void accumulate(Map<String, Reward> map, String key, double reward) {
        Reward current = map.get(key);
        int count = current != null ? current.count() : 0;
        double sum = current != null ? current.mean() * current.count() : 0.0;
        map.put(key, new Reward((sum + reward) / (count + 1), count + 1));
    }
}
