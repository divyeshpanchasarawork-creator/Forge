package com.forge.topic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topics")
public class Topic extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer confidence = 0;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer mastery = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_revision")
    private LocalDateTime lastRevision;

    @Column(name = "next_revision")
    private LocalDate nextRevision;

    @Column(length = 20)
    private String status = "NOT_STARTED";

    @Column(name = "revision_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer revisionCount = 0;

    @Column(name = "estimated_retention", columnDefinition = "DOUBLE DEFAULT 100.0")
    private Double estimatedRetention = 100.0;

    @Column(length = 20)
    private String source = "MANUAL";

    @Column(name = "easiness_factor", columnDefinition = "DOUBLE PRECISION DEFAULT 2.5")
    private Double easinessFactor = 2.5;

    @Column(name = "repetition_interval", columnDefinition = "INTEGER DEFAULT 0")
    private Integer repetitionInterval = 0;

    @Column(name = "skill_rating", columnDefinition = "DOUBLE PRECISION DEFAULT 1000")
    private Double skillRating = 1000.0;

    @Column(name = "memory_strength", columnDefinition = "DOUBLE PRECISION DEFAULT 1.0")
    private Double memoryStrength = 1.0;

    @Column(name = "attempts_solved", columnDefinition = "INTEGER DEFAULT 0")
    private Integer attemptsSolved = 0;

    @Column(name = "attempts_total", columnDefinition = "INTEGER DEFAULT 0")
    private Integer attemptsTotal = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;
}
