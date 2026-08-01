package com.forge.practice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProblemAttemptRequest {

    private String problemTitle;
    private String problemSlug;
    private String difficulty;
    private String topicTagSlug;
    private String topicTagName;
    private String outcome;
    private Integer hintsUsed;
    private Integer timeTakenSeconds;
}
