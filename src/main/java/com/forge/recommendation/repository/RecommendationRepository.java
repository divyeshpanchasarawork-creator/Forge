package com.forge.recommendation.repository;

import com.forge.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUserIdAndDismissedOrderByPriorityAscCreatedAtDesc(UUID userId, Boolean dismissed);

    @Transactional
    void deleteByUserIdAndDismissed(UUID userId, Boolean dismissed);
}
