package com.forge.problem.repository;

import com.forge.problem.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Page<Problem> findByUserId(UUID userId, Pageable pageable);

    Page<Problem> findByUserIdAndDifficulty(UUID userId, String difficulty, Pageable pageable);

    @Query("SELECT p FROM Problem p JOIN p.topics t WHERE p.user.id = :userId AND t.id = :topicId")
    Page<Problem> findByUserIdAndTopicId(@Param("userId") UUID userId, @Param("topicId") UUID topicId, Pageable pageable);

    @Query("SELECT p FROM Problem p JOIN p.topics t WHERE t.id = :topicId")
    Page<Problem> findByTopicId(@Param("topicId") UUID topicId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndDifficulty(UUID userId, String difficulty);
}
