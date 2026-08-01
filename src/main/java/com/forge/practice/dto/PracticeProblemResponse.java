package com.forge.practice.dto;

import com.forge.common.util.ProblemScorer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProblemResponse {

    private String title;
    private String titleSlug;
    private String difficulty;
    private String topicTag;
    private String reason;
    private String segment;
    private Integer score;
    private List<ProblemScorer.ScoreItem> breakdown;
    private Integer attempts;
    private Integer solved;
}
