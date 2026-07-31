package com.forge.analytics.controller;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.analytics.service.AnalyticsService;
import com.forge.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
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
}
