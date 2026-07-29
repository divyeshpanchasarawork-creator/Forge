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
        return recommendationRepository.findByUserIdAndDismissedOrderByPriorityAscCreatedAtDesc(userId, false)
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

    public RecommendationResponse dismissRecommendation(UUID id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation", "id", id));
        rec.setDismissed(true);
        rec = recommendationRepository.save(rec);
        return recommendationMapper.toResponse(rec);
    }
}
