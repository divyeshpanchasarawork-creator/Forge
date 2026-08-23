package com.forge.leetcode.repository;

import com.forge.leetcode.entity.ExternalSolve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExternalSolveRepository extends JpaRepository<ExternalSolve, UUID> {

    List<ExternalSolve> findByUserIdAndLoggedFalseOrderBySolvedAtDesc(UUID userId);

    List<ExternalSolve> findByUserIdAndTitleSlugIn(UUID userId, Collection<String> titleSlugs);

    boolean existsByUserIdAndTitleSlug(UUID userId, String titleSlug);

    @Modifying
    @Query("UPDATE ExternalSolve e SET e.logged = true WHERE e.user.id = :userId AND e.titleSlug = :slug AND e.logged = false")
    int markLogged(@Param("userId") UUID userId, @Param("slug") String slug);
}
