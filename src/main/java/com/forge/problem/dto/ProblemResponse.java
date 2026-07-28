package com.forge.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {

    private UUID id;
    private String title;
    private String leetcodeId;
    private String difficulty;
    private Integer timeTaken;
    private Integer attempts;
    private Integer confidence;
    private String mistakes;
    private String summary;
    private String notes;
    private String solutionUrl;
    private LocalDateTime solvedAt;
    private List<TopicInfo> topics;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicInfo {
        private UUID id;
        private String title;
        private String category;
    }
}
