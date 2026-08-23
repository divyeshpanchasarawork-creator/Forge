package com.forge.leetcode.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingSolveResponse(
        UUID id,
        String title,
        String titleSlug,
        String difficulty,
        String topicTagSlug,
        LocalDateTime solvedAt) {
}
