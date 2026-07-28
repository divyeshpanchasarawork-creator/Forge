package com.forge.recommendation.service;

import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.SecurityUtils;
import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.mapper.RecommendationMapper;
import com.forge.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationMapper recommendationMapper;

    public List<RecommendationResponse> getActiveRecommendations() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return recommendationRepository.findByUserIdAndDismissedOrderByPriorityAscCreatedAtDesc(userId, false).stream()
                .map(recommendationMapper::toResponse)
                .toList();
    }

    public RecommendationResponse dismissRecommendation(UUID id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation", "id", id));
        rec.setDismissed(true);
        rec = recommendationRepository.save(rec);
        log.info("Recommendation dismissed: {}", rec.getTitle());
        return recommendationMapper.toResponse(rec);
    }
}
