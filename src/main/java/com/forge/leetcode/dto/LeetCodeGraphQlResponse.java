package com.forge.leetcode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeetCodeGraphQlResponse {

    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private List<DifficultyCount> allQuestionsCount;
        private MatchedUser matchedUser;
        private UserContestRanking userContestRanking;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DifficultyCount {
        private String difficulty;
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchedUser {
        private SubmitStatsGlobal submitStatsGlobal;
        private List<BeatsStat> problemsSolvedBeatsStats;
        private UserCalendar userCalendar;
        private TagProblemCountGroup tagProblemCounts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitStatsGlobal {
        private List<DifficultyCount> acSubmissionNum;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BeatsStat {
        private String difficulty;
        private Double percentage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserCalendar {
        private Integer streak;
        private Integer totalActiveDays;
        private String submissionCalendar;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagProblemCountGroup {
        private List<TagCount> advanced;
        private List<TagCount> intermediate;
        private List<TagCount> fundamental;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagCount {
        @JsonProperty("tagName")
        private String tagName;
        @JsonProperty("tagSlug")
        private String tagSlug;
        private int problemsSolved;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserContestRanking {
        private Integer attendedContestsCount;
        private Double rating;
        private Integer globalRanking;
        private Double topPercentage;
    }
}
