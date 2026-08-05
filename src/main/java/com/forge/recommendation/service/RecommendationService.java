package com.forge.recommendation.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
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

    public List<RecommendationResponse> getActiveRecommendations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return recommendationRepository.findByUserIdAndStatusOrderByPriorityAscCreatedAtDesc(userId, Recommendation.STATUS_ACTIVE)
                .stream()
                .map(recommendationMapper::toResponse)
                .toList();
    }

    @Transactional
    public GenerateResponse generateRecommendations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        LocalDate today = LocalDate.now();
        if (user.getLastGenerationDate() == null || !user.getLastGenerationDate().equals(today)) {
            user.setDailyGenerationsUsed(0);
            user.setLastGenerationDate(today);
        }

        int used = user.getDailyGenerationsUsed() != null ? user.getDailyGenerationsUsed() : 0;
        if (used >= DAILY_LIMIT) {
            throw new BadRequestException("Daily generation limit reached (" + DAILY_LIMIT + "/" + DAILY_LIMIT + "). Try again tomorrow.");
        }

        List<Recommendation> recs = recommendationEngine.generateForUser(userId, true);
        user.setDailyGenerationsUsed(used + 1);
        userRepository.save(user);

        List<RecommendationResponse> responseRecs = recs.stream()
                .map(recommendationMapper::toResponse)
                .toList();

        return new GenerateResponse(responseRecs, DAILY_LIMIT - used - 1, DAILY_LIMIT);
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
        rec.setCompletedAt(LocalDateTime.now());
        rec.setOutcome(normalizeOutcome(outcome));
        rec = recommendationRepository.save(rec);
        return recommendationMapper.toResponse(rec);
    }

    @Transactional
    public void completeRecommendationsForProblem(UUID userId, String problemSlug, String outcome) {
        if (problemSlug == null || problemSlug.isBlank()) return;
        List<Recommendation> recs = recommendationRepository
                .findByUserIdAndStatusAndProblemSlug(userId, Recommendation.STATUS_ACTIVE, problemSlug);
        if (recs.isEmpty()) return;
        for (Recommendation rec : recs) {
            rec.setStatus(Recommendation.STATUS_COMPLETED);
            rec.setCompletedAt(LocalDateTime.now());
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
        rec.setDismissed(true);
        rec = recommendationRepository.save(rec);
        return recommendationMapper.toResponse(rec);
    }

    private Recommendation findOwnedRecommendation(UUID id, UUID userId) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation", "id", id));
        if (!rec.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Recommendation", "id", id);
        }
        return rec;
    }

    private String normalizeOutcome(String outcome) {
        if (outcome == null) return null;
        String normalized = outcome.toUpperCase();
        return List.of("SOLVED", "PARTIAL", "FAILED", "SKIPPED").contains(normalized) ? normalized : null;
    }
}
