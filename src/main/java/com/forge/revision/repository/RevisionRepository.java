package com.forge.revision.repository;

import com.forge.revision.entity.Revision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RevisionRepository extends JpaRepository<Revision, UUID> {

    @Query("SELECT r FROM Revision r JOIN FETCH r.topic WHERE r.id = :id")
    java.util.Optional<Revision> findByIdWithTopic(@Param("id") UUID id);

    @Query("SELECT r FROM Revision r JOIN FETCH r.topic WHERE r.user.id = :userId AND r.scheduledDate = :date AND r.completed = :completed")
    List<Revision> findByUserIdAndScheduledDateAndCompleted(@Param("userId") UUID userId, @Param("date") LocalDate date, @Param("completed") Boolean completed);

    @Query("SELECT r FROM Revision r JOIN FETCH r.topic WHERE r.user.id = :userId AND r.scheduledDate <= :date AND r.completed = false ORDER BY r.priority ASC, r.scheduledDate ASC")
    List<Revision> findPendingRevisionsByUserId(@Param("userId") UUID userId, @Param("date") LocalDate date);

    long countByUserIdAndCompleted(UUID userId, Boolean completed);

    @Query("SELECT COUNT(r) FROM Revision r WHERE r.user.id = :userId AND r.completed = true AND r.scheduledDate BETWEEN :startDate AND :endDate")
    long countCompletedInRangeByUserId(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT r.scheduledDate FROM Revision r WHERE r.user.id = :userId AND r.completed = true AND r.scheduledDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findCompletedDatesInRangeByUserId(@Param("userId") UUID userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByTopicIdAndCompletedFalse(UUID topicId);
}
