package com.forge.journal.controller;

import com.forge.common.dto.ApiResponse;
import com.forge.common.dto.PagedResponse;
import com.forge.journal.dto.JournalRequest;
import com.forge.journal.dto.JournalResponse;
import com.forge.journal.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JournalResponse>>> getJournals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(journalService.getJournals(page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JournalResponse>> createOrUpdateJournal(@Valid @RequestBody JournalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Journal saved", journalService.createOrUpdateJournal(request)));
    }
}
