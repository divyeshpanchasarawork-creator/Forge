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
    private Double averageMastery;
    private DifficultyBreakdown problemsByDifficulty;
    private List<CategoryMastery> masteryByCategory;
    private List<TopicSummary> weakestTopics;
    private List<TopicSummary> strongestTopics;
    private long currentStreak;
    private Integer targetLevel;
    private Integer readinessScore;

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
    public static class TopicSummary {
        private String title;
        private Integer confidence;
        private Integer mastery;
        private String category;
    }
}
