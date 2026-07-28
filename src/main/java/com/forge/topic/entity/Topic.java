package com.forge.topic.entity;

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
@Table(name = "topics")
public class Topic extends BaseEntity {

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
    private LocalDateTime nextRevision;

    @Column(length = 20)
    private String status = "NOT_STARTED";

    @Column(name = "revision_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer revisionCount = 0;

    @Column(name = "estimated_retention", columnDefinition = "DOUBLE DEFAULT 100.0")
    private Double estimatedRetention = 100.0;

    @Column(length = 20)
    private String source = "MANUAL";
}
