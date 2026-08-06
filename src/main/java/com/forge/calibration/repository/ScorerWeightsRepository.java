package com.forge.calibration.repository;

import com.forge.calibration.entity.ScorerWeights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScorerWeightsRepository extends JpaRepository<ScorerWeights, UUID> {

    Optional<ScorerWeights> findFirstByOrderByCreatedAtDesc();
}
