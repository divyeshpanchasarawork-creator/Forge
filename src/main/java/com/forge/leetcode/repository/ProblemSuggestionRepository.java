package com.forge.leetcode.repository;

import com.forge.leetcode.entity.ProblemSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProblemSuggestionRepository extends JpaRepository<ProblemSuggestion, UUID> {

    List<ProblemSuggestion> findByUserId(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProblemSuggestion s where s.user.id = :userId and s.source = :source")
    void deleteByUserIdAndSource(@Param("userId") UUID userId, @Param("source") String source);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProblemSuggestion s where s.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
