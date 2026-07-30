package com.forge.roadmap.dto;

import java.util.List;

public record RoadmapAnalysisResponse(
        String paragraph,
        int currentLevel,
        String focusArea,
        String estimatedTimeToNextLevel,
        List<TagInfo> strongTags,
        List<TagInfo> weakTags,
        String nextMilestone,
        int readinessScore,
        String recommendedDifficultySplit
) {
    public record TagInfo(String name, String slug, int solved, int confidence) {}
}
