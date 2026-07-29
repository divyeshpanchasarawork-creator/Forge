package com.forge.leetcode.repository;

import com.forge.leetcode.entity.ProblemSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProblemSuggestionRepository extends JpaRepository<ProblemSuggestion, UUID> {

    List<ProblemSuggestion> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
