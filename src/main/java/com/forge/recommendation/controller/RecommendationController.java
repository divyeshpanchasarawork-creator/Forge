package com.forge.recommendation.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.recommendation.dto.CompleteRequest;
import com.forge.recommendation.dto.GenerateResponse;
import com.forge.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateResponse>> generateRecommendations() {
        GenerateResponse response = recommendationService.generateRecommendations();
        return ResponseEntity.ok(ApiResponse.success("Recommendations generated", response));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<?>> completeRecommendation(@PathVariable UUID id,
                                                                 @RequestBody(required = false) CompleteRequest request) {
        String outcome = request != null ? request.getOutcome() : null;
        return ResponseEntity.ok(ApiResponse.success("Recommendation completed", recommendationService.completeRecommendation(id, outcome)));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<?>> dismissRecommendation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Recommendation dismissed", recommendationService.dismissRecommendation(id)));
    }
}
