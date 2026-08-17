package com.forge.journal.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.common.dto.PagedResponse;
import com.forge.journal.dto.JournalRequest;
import com.forge.journal.dto.JournalResponse;
import com.forge.journal.service.JournalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
@Validated
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JournalResponse>>> getJournals(
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(journalService.getJournals(page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JournalResponse>> createOrUpdateJournal(@Valid @RequestBody JournalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Journal saved", journalService.createOrUpdateJournal(request)));
    }
}
