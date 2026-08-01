package com.forge.practice.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.practice.dto.PracticeQueueResponse;
import com.forge.practice.dto.ProblemAttemptRequest;
import com.forge.practice.dto.ProblemAttemptResponse;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<PracticeQueueResponse>> getQueue() {
        return ResponseEntity.ok(ApiResponse.success(practiceService.getPracticeQueue()));
    }

    @PostMapping("/attempts")
    public ResponseEntity<ApiResponse<ProblemAttemptResponse>> submitAttempt(@Valid @RequestBody ProblemAttemptRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Attempt recorded", practiceService.submitAttempt(request)));
    }

    @GetMapping("/attempts")
    public ResponseEntity<ApiResponse<List<ProblemAttempt>>> getAttempts(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(practiceService.getAttemptHistory(limit)));
    }
}
