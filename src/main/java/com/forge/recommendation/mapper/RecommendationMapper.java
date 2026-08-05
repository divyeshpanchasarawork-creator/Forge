package com.forge.recommendation.mapper;

import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.entity.Recommendation;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMapper {

    public RecommendationResponse toResponse(Recommendation rec) {
        return new RecommendationResponse(
                rec.getId(),
                rec.getTitle(),
                rec.getDescription(),
                rec.getReason(),
                rec.getPriority(),
                rec.getAction(),
                rec.getDismissed(),
                rec.getStatus(),
                rec.getCompletedAt(),
                rec.getOutcome(),
                rec.getProblemSlug(),
                rec.getProblemTitle(),
                rec.getProblemDifficulty(),
                rec.getCreatedAt(),
                null
        );
    }
}
