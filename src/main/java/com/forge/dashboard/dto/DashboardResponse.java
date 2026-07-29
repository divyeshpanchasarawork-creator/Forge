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
    private String recentJournal;
    private List<String> recentProblems;
    private LeetCodeStats leetcodeStats;
    private TargetProgress targetProgress;
    private List<KnowledgeMapCategory> knowledgeMap;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeHealth {
        private Integer averageMastery;
        private Integer averageConfidence;
        private Double averageRetention;
        private long totalTopics;
        private long masteredTopics;
        private long inProgressTopics;
        private long notStartedTopics;
        private long overdueRevisions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetProgress {
        private int targetLevel;
        private int readinessScore;
        private int totalSolved;
        private int targetTotal;
        private DifficultyGap difficultyGap;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyGap {
        private int currentEasy;
        private int currentMedium;
        private int currentHard;
        private int targetEasy;
        private int targetMedium;
        private int targetHard;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeMapCategory {
        private String category;
        private List<TopicSummary> topics;
        private int averageMastery;
        private int averageConfidence;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicSummary {
        private String id;
        private String title;
        private int mastery;
        private int confidence;
        private String status;
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
