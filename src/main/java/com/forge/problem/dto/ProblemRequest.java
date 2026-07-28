package com.forge.problem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class ProblemRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String leetcodeId;

    @NotBlank(message = "Difficulty is required")
    private String difficulty;

    private Integer timeTaken;
    private Integer attempts;
    private Integer confidence;
    private String mistakes;
    private String summary;
    private String notes;
    private String solutionUrl;
    private LocalDateTime solvedAt;
    private Set<UUID> topicIds;
}
