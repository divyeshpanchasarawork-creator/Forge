package com.forge.recommendation.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TimezoneUtil;
import com.forge.recommendation.dto.GenerateResponse;
import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.mapper.RecommendationMapper;
import com.forge.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DAILY_LIMIT = 4;

    private final RecommendationRepository recommendationRepository;
    private final RecommendationEngine recommendationEngine;
    private final RecommendationMapper recommendationMapper;
    private final UserRepository userRepository;
    private final ProblemScorer problemScorer;
    private final ProblemLoader problemLoader;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getActiveRecommendations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return toResponses(recommendationRepository.findByUserIdAndStatusOrderByPriorityAscCreatedAtDesc(userId, Recommendation.STATUS_ACTIVE));
    }

    @Transactional
    public GenerateResponse generateRecommendations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        LocalDate today = LocalDate.now(TimezoneUtil.resolve(user));
        int reserved = userRepository.reserveDailyGeneration(userId, today, DAILY_LIMIT);
        if (reserved == 0) {
            throw new BadRequestException("Daily generation limit reached (" + DAILY_LIMIT + "/" + DAILY_LIMIT + "). Try again tomorrow.");
        }

        try {
            List<Recommendation> recs = recommendationEngine.generateForUser(userId, true);
            int used = userRepository.findDailyGenerationsUsed(userId);
            List<RecommendationResponse> responseRecs = toResponses(recs);
            return new GenerateResponse(responseRecs, Math.max(0, DAILY_LIMIT - used), DAILY_LIMIT);
        } catch (RuntimeException e) {
            userRepository.releaseDailyGeneration(userId, today);
            throw e;
        }
    }

    @Transactional
    public RecommendationResponse completeRecommendation(UUID id, String outcome) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Recommendation rec = findOwnedRecommendation(id, userId);

        if (Recommendation.STATUS_COMPLETED.equals(rec.getStatus())) {
            throw new BadRequestException("Recommendation already completed");
        }
        if (Recommendation.STATUS_DISMISSED.equals(rec.getStatus())) {
            throw new BadRequestException("Recommendation was dismissed");
        }

        rec.setStatus(Recommendation.STATUS_COMPLETED);
        rec.setCompletedAt(TimezoneUtil.now(rec.getUser()));
        rec.setOutcome(normalizeOutcome(outcome));
        rec = recommendationRepository.save(rec);
        return toResponse(rec, problemScorer.context(userId));
    }

    @Transactional
    public void completeRecommendationsForProblem(UUID userId, String problemSlug, String outcome) {
        if (problemSlug == null || problemSlug.isBlank()) return;
        List<Recommendation> recs = recommendationRepository
                .findByUserIdAndStatusAndProblemSlug(userId, Recommendation.STATUS_ACTIVE, problemSlug);
        if (recs.isEmpty()) return;
        LocalDateTime now = TimezoneUtil.now(recs.getFirst().getUser());
        for (Recommendation rec : recs) {
            rec.setStatus(Recommendation.STATUS_COMPLETED);
            rec.setCompletedAt(now);
            rec.setOutcome(normalizeOutcome(outcome));
        }
        recommendationRepository.saveAll(recs);
    }

    @Transactional
    public RecommendationResponse dismissRecommendation(UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Recommendation rec = findOwnedRecommendation(id, userId);
        if (Recommendation.STATUS_COMPLETED.equals(rec.getStatus())) {
            throw new BadRequestException("Recommendation already completed");
        }
        rec.setStatus(Recommendation.STATUS_DISMISSED);
        rec = recommendationRepository.save(rec);
        return toResponse(rec, problemScorer.context(userId));
    }

    private Recommendation findOwnedRecommendation(UUID id, UUID userId) {
        return recommendationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation", "id", id));
    }

    private String normalizeOutcome(String outcome) {
        if (outcome == null) return null;
        String normalized = outcome.toUpperCase();
        return List.of("SOLVED", "PARTIAL", "FAILED", "SKIPPED").contains(normalized) ? normalized : null;
    }

    private List<RecommendationResponse> toResponses(List<Recommendation> recs) {
        ProblemScorer.ScoringContext ctx = problemScorer.context(SecurityUtils.getCurrentUserId());
        return recs.stream()
                .map(rec -> toResponse(rec, ctx))
                .sorted(Comparator
                        .comparing(RecommendationResponse::getScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RecommendationResponse::getPriority,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RecommendationResponse::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private RecommendationResponse toResponse(Recommendation rec, ProblemScorer.ScoringContext ctx) {
        RecommendationResponse resp = recommendationMapper.toResponse(rec);
        if (rec.getProblemSlug() != null) {
            String tag = problemLoader.getTagSlugForProblem(rec.getProblemSlug());
            if (tag != null) {
                ProblemLoader.ProblemEntry entry = new ProblemLoader.ProblemEntry(
                        rec.getProblemTitle(), rec.getProblemSlug(), rec.getProblemDifficulty());
                resp.setScoreBreakdown(problemScorer.breakdown(ctx, entry, tag));
                resp.setScore(resp.getScoreBreakdown() != null ? resp.getScoreBreakdown().total() : null);
            }
        }
        return resp;
    }
}
