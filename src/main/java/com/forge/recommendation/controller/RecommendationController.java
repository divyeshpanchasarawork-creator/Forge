package com.forge.recommendation.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations() {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getActiveRecommendations()));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> generateRecommendations() {
        List<RecommendationResponse> recs = recommendationService.generateRecommendations();
        return ResponseEntity.ok(ApiResponse.success("Recommendations generated", recs));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<RecommendationResponse>> dismissRecommendation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Recommendation dismissed", recommendationService.dismissRecommendation(id)));
    }
}
