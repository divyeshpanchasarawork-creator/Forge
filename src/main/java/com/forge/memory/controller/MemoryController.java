package com.forge.memory.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.memory.dto.MemoryResponse;
import com.forge.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<MemoryResponse>> getMemory() {
        return ResponseEntity.ok(ApiResponse.success(memoryService.getMemory()));
    }
}
