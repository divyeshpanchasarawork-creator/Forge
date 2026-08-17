package com.forge.analytics.controller;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.dto.ActivityDay;
import com.forge.analytics.dto.LearningCurveResponse;
import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.analytics.service.AnalyticsService;
import com.forge.common.dto.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Validated
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAnalytics()));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyProgressResponse>> getWeeklyProgress() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getWeeklyProgress()));
    }

    @GetMapping("/learning-curve")
    public ResponseEntity<ApiResponse<LearningCurveResponse>> getLearningCurve(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getLearningCurve(days)));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ApiResponse<List<ActivityDay>>> getHeatmap(
            @RequestParam(defaultValue = "28") @Min(1) @Max(104) int weeks) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getActivityHeatmap(weeks)));
    }
}
