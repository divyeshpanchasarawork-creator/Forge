package com.forge.recommendation.repository;

import com.forge.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByUserIdAndStatusOrderByPriorityAscCreatedAtDesc(UUID userId, String status);

    List<Recommendation> findByUserIdAndStatusAndProblemSlug(UUID userId, String status, String problemSlug);

    Optional<Recommendation> findByIdAndUserId(UUID id, UUID userId);

    @Transactional
    void deleteByUserIdAndStatus(UUID userId, String status);
}
