package com.forge.roadmap.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.roadmap.dto.RoadmapAnalysisResponse;
import com.forge.roadmap.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @GetMapping("/analysis")
    public ResponseEntity<ApiResponse<RoadmapAnalysisResponse>> getAnalysis() {
        RoadmapAnalysisResponse analysis = roadmapService.getAnalysis();
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }
}
