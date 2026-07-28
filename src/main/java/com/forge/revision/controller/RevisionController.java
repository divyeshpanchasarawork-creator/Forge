package com.forge.revision.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.revision.dto.RevisionResponse;
import com.forge.revision.service.RevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/revisions")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<RevisionResponse>>> getTodayRevisions() {
        return ResponseEntity.ok(ApiResponse.success(revisionService.getTodayRevisions()));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<RevisionResponse>>> getPendingRevisions() {
        return ResponseEntity.ok(ApiResponse.success(revisionService.getPendingRevisions()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<RevisionResponse>> completeRevision(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Revision completed", revisionService.completeRevision(id)));
    }
}
