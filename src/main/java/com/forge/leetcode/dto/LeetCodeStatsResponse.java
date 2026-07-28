package com.forge.leetcode.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeetCodeStatsResponse {

    private Integer totalSolved;
    private Integer easySolved;
    private Integer mediumSolved;
    private Integer hardSolved;
    private Double easyBeatsPct;
    private Double mediumBeatsPct;
    private Double hardBeatsPct;
    private Integer ranking;
    private Double contestRating;
    private Integer contestRanking;
    private Integer contestAttendedCount;
    private Integer streak;
    private Integer totalActiveDays;
    private LocalDateTime lastSyncedAt;
    private List<TagStat> tags;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagStat {
        private String tagName;
        private String tagSlug;
        private Integer problemsSolved;
        private String skillLevel;
    }
}
