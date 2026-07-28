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

import java.util.List;

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

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<JournalResponse>> getTodayJournal() {
        return ResponseEntity.ok(ApiResponse.success(journalService.getTodayJournal()));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<JournalResponse>>> getRecentJournals() {
        return ResponseEntity.ok(ApiResponse.success(journalService.getRecentJournals()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JournalResponse>> createOrUpdateJournal(@Valid @RequestBody JournalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Journal saved", journalService.createOrUpdateJournal(request)));
    }
}
