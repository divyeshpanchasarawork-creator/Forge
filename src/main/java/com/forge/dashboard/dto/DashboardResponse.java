package com.forge.dashboard.dto;

import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.revision.dto.RevisionResponse;
import com.forge.topic.dto.TopicResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private String greeting;
    private String currentFocus;
    private String todayMission;
    private List<RevisionResponse> revisionsDue;
    private List<RecommendationResponse> recommendations;
    private List<TopicResponse> weakTopics;
    private List<TopicResponse> strongTopics;
    private KnowledgeHealth knowledgeHealth;
    private WeeklyProgress weeklyProgress;
    private String recentJournal;
    private List<String> recentProblems;
    private LeetCodeStats leetcodeStats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeHealth {
        private Integer averageMastery;
        private Integer averageConfidence;
        private long totalTopics;
        private long masteredTopics;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyProgress {
        private long problemsSolved;
        private long topicsReviewed;
        private Double hoursStudied;
        private long revisionsCompleted;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeetCodeStats {
        private Integer totalSolved;
        private Integer easySolved;
        private Integer mediumSolved;
        private Integer hardSolved;
        private Integer ranking;
        private Integer streak;
        private Integer totalActiveDays;
        private Double contestRating;
        private Integer contestRanking;
        private Integer contestAttendedCount;
        private java.time.LocalDateTime lastSyncedAt;
    }
}
