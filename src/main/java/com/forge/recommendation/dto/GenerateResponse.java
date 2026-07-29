package com.forge.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GenerateResponse {
    private List<RecommendationResponse> recommendations;
    private int remainingGenerations;
    private int dailyLimit;
}
