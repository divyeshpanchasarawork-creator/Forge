package com.forge.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private UUID id;
    private String title;
    private String description;
    private String reason;
    private Integer priority;
    private String action;
    private Boolean dismissed;
    private String problemSlug;
    private String problemTitle;
    private String problemDifficulty;
    private LocalDateTime createdAt;
}
