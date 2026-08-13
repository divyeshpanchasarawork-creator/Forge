package com.forge.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProblemAttemptRequest {

    @NotBlank(message = "problemTitle is required")
    private String problemTitle;
    @NotBlank(message = "problemSlug is required")
    private String problemSlug;
    @Size(max = 10, message = "difficulty must be at most 10 characters")
    private String difficulty;
    private String topicTagSlug;
    private String topicTagName;
    @NotBlank(message = "outcome is required")
    private String outcome;
    @Min(value = 0, message = "hintsUsed must be between 0 and 3")
    @Max(value = 3, message = "hintsUsed must be between 0 and 3")
    private Integer hintsUsed;
    @Min(value = 0, message = "timeTakenSeconds cannot be negative")
    @Max(value = 86400, message = "timeTakenSeconds must be at most 86400")
    private Integer timeTakenSeconds;
}
