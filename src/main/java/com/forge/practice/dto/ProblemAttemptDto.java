package com.forge.practice.dto;

import com.forge.practice.entity.ProblemAttempt;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProblemAttemptDto(
        UUID id,
        String problemTitle,
        String problemSlug,
        String difficulty,
        String topicTagSlug,
        String topicTagName,
        String outcome,
        Integer hintsUsed,
        Integer timeTakenSeconds,
        Integer quality,
        LocalDateTime attemptedAt
) {
    public static ProblemAttemptDto from(ProblemAttempt attempt) {
        return new ProblemAttemptDto(
                attempt.getId(),
                attempt.getProblemTitle(),
                attempt.getProblemSlug(),
                attempt.getDifficulty(),
                attempt.getTopicTagSlug(),
                attempt.getTopicTagName(),
                attempt.getOutcome(),
                attempt.getHintsUsed(),
                attempt.getTimeTakenSeconds(),
                attempt.getQuality(),
                attempt.getAttemptedAt());
    }
}
