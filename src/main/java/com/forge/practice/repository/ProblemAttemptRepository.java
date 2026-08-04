package com.forge.practice.repository;

import com.forge.practice.entity.ProblemAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemAttemptRepository extends JpaRepository<ProblemAttempt, UUID> {

    List<ProblemAttempt> findByUserIdOrderByAttemptedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndAttemptedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndOutcome(UUID userId, String outcome);

    long countByUserId(UUID userId);

    long countByUserIdAndOutcomeAndAttemptedAtBetween(UUID userId, String outcome, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM ProblemAttempt a WHERE a.user.id = :userId ORDER BY a.attemptedAt DESC")
    List<ProblemAttempt> findByUserIdAll(@Param("userId") UUID userId);

    @Query("SELECT COUNT(DISTINCT a.problemSlug) FROM ProblemAttempt a WHERE a.user.id = :userId")
    long countDistinctProblemsByUserId(@Param("userId") UUID userId);

    @Query("SELECT a.attemptedAt FROM ProblemAttempt a WHERE a.user.id = :userId AND a.attemptedAt BETWEEN :start AND :end")
    List<LocalDateTime> findAttemptedAtInRangeByUserId(@Param("userId") UUID userId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("SELECT a FROM ProblemAttempt a WHERE a.user.id = :userId AND a.problemSlug = :problemSlug ORDER BY a.attemptedAt DESC")
    List<ProblemAttempt> findByUserIdAndProblemSlug(@Param("userId") UUID userId, @Param("problemSlug") String problemSlug);

    Optional<ProblemAttempt> findFirstByUserIdAndOutcomeAndDifficultyOrderByAttemptedAtAsc(UUID userId, String outcome, String difficulty);
}
