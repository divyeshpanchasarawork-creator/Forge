package com.forge.topic.repository;

import com.forge.topic.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Page<Topic> findByUserId(UUID userId, Pageable pageable);

    Page<Topic> findByUserIdAndCategory(UUID userId, String category, Pageable pageable);

    Page<Topic> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND t.confidence < 4 ORDER BY t.confidence ASC")
    List<Topic> findWeakTopicsByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND t.confidence >= 7 ORDER BY t.confidence DESC")
    List<Topic> findStrongTopicsByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND t.nextRevision IS NOT NULL AND t.nextRevision <= CURRENT_TIMESTAMP ORDER BY t.nextRevision ASC")
    List<Topic> findTopicsNeedingRevisionByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndCategory(UUID userId, String category);

    List<Topic> findByUserIdAndSource(UUID userId, String source);
}
