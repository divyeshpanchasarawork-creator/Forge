package com.forge.auth.repository;

import com.forge.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByPreferredAnalysisTimeIsNotNull();

    /**
     * Atomically reserves one daily recommendation generation, resetting the counter when the
     * day has rolled over. Returns 1 when the slot was reserved, 0 when the daily limit is reached.
     */
    @Modifying
    @Query("UPDATE User u SET u.dailyGenerationsUsed = " +
            "CASE WHEN (u.lastGenerationDate IS NULL OR u.lastGenerationDate <> :today) THEN 1 " +
            "ELSE u.dailyGenerationsUsed + 1 END, " +
            "u.lastGenerationDate = :today " +
            "WHERE u.id = :userId AND (u.lastGenerationDate IS NULL OR u.lastGenerationDate <> :today OR u.dailyGenerationsUsed < :limit)")
    int reserveDailyGeneration(@Param("userId") UUID userId, @Param("today") LocalDate today, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE User u SET u.dailyGenerationsUsed = u.dailyGenerationsUsed - 1 " +
            "WHERE u.id = :userId AND u.dailyGenerationsUsed > 0 AND u.lastGenerationDate = :today")
    int releaseDailyGeneration(@Param("userId") UUID userId, @Param("today") LocalDate today);

    /**
     * Fresh read of the daily-generation counter straight from the DB. A projection query
     * bypasses the first-level entity cache, so it reflects bulk @Modifying updates that the
     * managed entity does not.
     */
    @Query("SELECT u.dailyGenerationsUsed FROM User u WHERE u.id = :userId")
    int findDailyGenerationsUsed(@Param("userId") UUID userId);
}
