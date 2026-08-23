package com.forge.calibration.entity;

import com.forge.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Immutable ledger row written by every {@link com.forge.calibration.service.CalibrationJob}
 * run — applied, kept-weights, or skipped — so the engine health card can show a trend
 * instead of only the latest state (the {@link ScorerWeights} row is overwritten in place
 * and keeps no history).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "calibration_runs")
public class CalibrationRun extends BaseEntity {

    @Column(name = "ran_at", nullable = false)
    private LocalDateTime ranAt;

    /** SKIPPED or APPLIED — mirrors CalibrationResult.status; a run that kept weights is SKIPPED. */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount = 0;

    @Column(name = "min_samples", nullable = false)
    private Integer minSamples = 0;

    /** Holdout MSE of the incumbent weights; null when the run never got to evaluation. */
    @Column(name = "metric_before")
    private Double metricBefore;

    /** Holdout MSE of the candidate weights; null when the run never got to evaluation. */
    @Column(name = "metric_after")
    private Double metricAfter;

    @Column(name = "swapped", nullable = false)
    private Boolean swapped = false;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
}
