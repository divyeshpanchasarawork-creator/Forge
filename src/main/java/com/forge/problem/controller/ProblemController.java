package com.forge.problem.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.common.dto.PagedResponse;
import com.forge.problem.dto.ProblemRequest;
import com.forge.problem.dto.ProblemResponse;
import com.forge.problem.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProblemResponse>>> getProblems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) UUID topicId) {
        return ResponseEntity.ok(ApiResponse.success(problemService.getProblems(page, size, difficulty, topicId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProblemResponse>> createProblem(@Valid @RequestBody ProblemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Problem created", problemService.createProblem(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProblemResponse>> updateProblem(@PathVariable UUID id, @Valid @RequestBody ProblemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Problem updated", problemService.updateProblem(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProblem(@PathVariable UUID id) {
        problemService.deleteProblem(id);
        return ResponseEntity.ok(ApiResponse.success("Problem deleted", null));
    }
}
