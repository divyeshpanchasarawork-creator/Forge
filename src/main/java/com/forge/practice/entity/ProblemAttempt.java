package com.forge.practice.entity;

import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "problem_attempts")
public class ProblemAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "problem_title", nullable = false, length = 255)
    private String problemTitle;

    @Column(name = "problem_slug", nullable = false, length = 255)
    private String problemSlug;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "topic_tag_slug", length = 100)
    private String topicTagSlug;

    @Column(name = "topic_tag_name", length = 100)
    private String topicTagName;

    @Column(nullable = false, length = 20)
    private String outcome = "SOLVED";

    @Column(name = "hints_used", columnDefinition = "INTEGER DEFAULT 0")
    private Integer hintsUsed = 0;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer quality = 0;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
}
