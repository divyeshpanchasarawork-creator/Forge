package com.forge.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private long totalProblems;
    private long totalTopics;
    private Double totalStudyHours;
    private Double averageMastery;
    private Integer averageConfidence;
    private DifficultyBreakdown problemsByDifficulty;
    private List<CategoryMastery> masteryByCategory;
    private Double revisionCompletionRate;
    private List<LearningTrendPoint> learningTrend;
    private List<TopicSummary> weakestTopics;
    private List<TopicSummary> strongestTopics;
    private long currentStreak;
    private LeetCodeOverview leetcodeOverview;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyBreakdown {
        private long easy;
        private long medium;
        private long hard;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMastery {
        private String category;
        private Integer averageMastery;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningTrendPoint {
        private String date;
        private long problemsSolved;
        private Double hoursStudied;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicSummary {
        private String title;
        private Integer confidence;
        private Integer mastery;
        private String category;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetCodeOverview {
        private Integer totalSolved;
        private Integer easySolved;
        private Integer mediumSolved;
        private Integer hardSolved;
        private Integer ranking;
        private Integer streak;
        private Integer totalActiveDays;
        private Double easyBeatsPct;
        private Double mediumBeatsPct;
        private Double hardBeatsPct;
    }
}
