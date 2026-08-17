package com.forge.practice.dto;

import com.forge.common.util.DifficultyUtil;
import com.forge.practice.entity.ProblemAttempt;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProblemAttemptSummary(
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
        LocalDateTime attemptedAt) {

    public static ProblemAttemptSummary from(ProblemAttempt attempt) {
        return new ProblemAttemptSummary(
                attempt.getId(),
                attempt.getProblemTitle(),
                attempt.getProblemSlug(),
                DifficultyUtil.titleCase(attempt.getDifficulty()),
                attempt.getTopicTagSlug(),
                attempt.getTopicTagName(),
                attempt.getOutcome(),
                attempt.getHintsUsed(),
                attempt.getTimeTakenSeconds(),
                attempt.getQuality(),
                attempt.getAttemptedAt());
    }
}
