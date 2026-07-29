package com.forge.common.util;

import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.topic.entity.Topic;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class ReadinessCalculator {

    public static int computeReadinessScore(int targetLevel, List<Topic> allTopics, LeetCodeSnapshot snapshot) {
        int totalSolved = 0;
        int easy = 0;
        int medium = 0;
        int hard = 0;
        if (snapshot != null) {
            totalSolved = snapshot.getTotalSolved() != null ? snapshot.getTotalSolved() : 0;
            easy = snapshot.getEasySolved() != null ? snapshot.getEasySolved() : 0;
            medium = snapshot.getMediumSolved() != null ? snapshot.getMediumSolved() : 0;
            hard = snapshot.getHardSolved() != null ? snapshot.getHardSolved() : 0;
        }

        int targetTotal = getTargetTotal(targetLevel);
        int targetHPct = getTargetHardPct(targetLevel);
        int targetMPct = getTargetMediumPct(targetLevel);
        int targetEPct = getTargetEasyPct(targetLevel);

        int targetHardTotal = (targetHPct * targetTotal) / 100;
        int targetMediumTotal = (targetMPct * targetTotal) / 100;
        int targetEasyTotal = (targetEPct * targetTotal) / 100;

        double problemScore = Math.min(100.0, (double) totalSolved / targetTotal * 100);
        double easyScore = targetEasyTotal > 0 ? Math.min(100.0, (double) easy / targetEasyTotal * 100) : 100.0;
        double mediumScore = targetMediumTotal > 0 ? Math.min(100.0, (double) medium / targetMediumTotal * 100) : 100.0;
        double hardScore = targetHardTotal > 0 ? Math.min(100.0, (double) hard / targetHardTotal * 100) : 100.0;
        double topicScore = allTopics.isEmpty() ? 0 :
                (double) allTopics.stream().filter(t -> t.getConfidence() >= 5).count() / allTopics.size() * 100;

        double readiness = (problemScore * 0.30) + ((easyScore + mediumScore + hardScore) / 3.0 * 0.35) + (topicScore * 0.35);
        return (int) Math.round(Math.min(100, readiness));
    }

    public static int getTargetTotal(int level) {
        if (level <= 2) return level <= 1 ? 50 : 80;
        if (level <= 4) return level == 3 ? 120 : 180;
        if (level <= 6) return level == 5 ? 250 : 320;
        if (level <= 8) return level == 7 ? 400 : 500;
        return level == 9 ? 600 : 800;
    }

    public static int getTargetHardPct(int level) {
        if (level <= 2) return 0;
        if (level <= 4) return level == 3 ? 10 : 15;
        if (level <= 6) return level == 5 ? 25 : 35;
        if (level <= 8) return level == 7 ? 50 : 60;
        return level == 9 ? 70 : 80;
    }

    public static int getTargetMediumPct(int level) {
        if (level <= 2) return level == 1 ? 20 : 30;
        if (level <= 4) return level == 3 ? 40 : 50;
        if (level <= 6) return level == 5 ? 55 : 50;
        if (level <= 8) return level == 7 ? 40 : 35;
        return level == 9 ? 25 : 20;
    }

    public static int getTargetEasyPct(int level) {
        return 100 - getTargetHardPct(level) - getTargetMediumPct(level);
    }
}
