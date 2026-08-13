package com.forge.analytics.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forge.auth.entity.User;
import com.forge.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "daily_metrics")
public class DailyMetric extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private Double mastery = 0.0;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private Double confidence = 0.0;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 100")
    private Double retention = 100.0;

    @Column(name = "skill_rating", columnDefinition = "DOUBLE PRECISION DEFAULT 1000")
    private Double skillRating = 1000.0;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private Double consistency = 0.0;

    @Column(name = "solved_delta", columnDefinition = "INTEGER DEFAULT 0")
    private Integer solvedDelta = 0;

    @Column(name = "revisions_done", columnDefinition = "INTEGER DEFAULT 0")
    private Integer revisionsDone = 0;

    @Column(name = "journal_hours", columnDefinition = "DOUBLE PRECISION DEFAULT 0")
    private Double journalHours = 0.0;
}
