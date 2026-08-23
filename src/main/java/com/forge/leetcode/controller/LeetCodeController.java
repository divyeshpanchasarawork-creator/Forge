package com.forge.leetcode.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.common.util.SecurityUtils;
import com.forge.leetcode.dto.LeetCodeStatsResponse;
import com.forge.leetcode.dto.PendingSolveResponse;
import com.forge.leetcode.service.LeetCodeFetchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leetcode")
@RequiredArgsConstructor
public class LeetCodeController {

    private final LeetCodeFetchService leetCodeFetchService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<LeetCodeStatsResponse>> sync() {
        UUID userId = SecurityUtils.getCurrentUserId();
        LeetCodeStatsResponse stats = leetCodeFetchService.syncUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("LeetCode profile synced", stats));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<LeetCodeStatsResponse>> getStats() {
        UUID userId = SecurityUtils.getCurrentUserId();
        LeetCodeStatsResponse stats = leetCodeFetchService.getLatestStats(userId);
        return ResponseEntity.ok(ApiResponse.success("LeetCode stats fetched", stats));
    }

    @GetMapping("/pending-solves")
    public ResponseEntity<ApiResponse<List<PendingSolveResponse>>> getPendingSolves() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<PendingSolveResponse> pending = leetCodeFetchService.getPendingSolves(userId);
        return ResponseEntity.ok(ApiResponse.success("Pending solves fetched", pending));
    }
}
