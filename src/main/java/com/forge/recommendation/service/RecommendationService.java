package com.forge.recommendation.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.recommendation.dto.GenerateResponse;
import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.mapper.RecommendationMapper;
import com.forge.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DAILY_LIMIT = 4;

    private final RecommendationRepository recommendationRepository;
    private final RecommendationEngine recommendationEngine;
    private final RecommendationMapper recommendationMapper;
    private final UserRepository userRepository;
    private final ProblemSuggestionRepository problemSuggestionRepository;

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
        syncRecProblemsToSuggestions(userId);
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

    @Transactional
    public void syncRecProblemsToSuggestions(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        List<Recommendation> recsWithProblems = recommendationRepository
                .findByUserIdAndDismissedOrderByPriorityAscCreatedAtDesc(userId, false)
                .stream()
                .filter(r -> r.getProblemSlug() != null)
                .toList();

        if (recsWithProblems.isEmpty()) return;

        Set<String> existingSlugs = new HashSet<>();
        problemSuggestionRepository.findByUserId(userId)
                .forEach(ps -> existingSlugs.add(ps.getTitleSlug()));

        List<ProblemSuggestion> toSave = new ArrayList<>();
        for (Recommendation rec : recsWithProblems) {
            if (!existingSlugs.contains(rec.getProblemSlug())) {
                ProblemSuggestion suggestion = new ProblemSuggestion();
                suggestion.setUser(user);
                suggestion.setTitle(rec.getProblemTitle());
                suggestion.setTitleSlug(rec.getProblemSlug());
                suggestion.setDifficulty(rec.getProblemDifficulty());
                toSave.add(suggestion);
                existingSlugs.add(rec.getProblemSlug());
            }
        }

        if (!toSave.isEmpty()) {
            problemSuggestionRepository.saveAll(toSave);
            log.info("Synced {} recommendation-linked problems to suggestions for user {}", toSave.size(), userId);
        }
    }
}
