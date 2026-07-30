package com.forge.practice.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<PracticeProblemResponse>>> getQueue() {
        return ResponseEntity.ok(ApiResponse.success(practiceService.getPracticeQueue()));
    }
}
