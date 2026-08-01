package com.forge.search.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.search.dto.ProblemSearchItem;
import com.forge.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/problems")
    public ResponseEntity<ApiResponse<List<ProblemSearchItem>>> searchProblems(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(searchService.searchProblems(q)));
    }
}
