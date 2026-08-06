package com.forge.calibration.entity;

import com.forge.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scorer_weights")
public class ScorerWeights extends BaseEntity {

    @Column(name = "weights_json", nullable = false, columnDefinition = "TEXT")
    private String weightsJson;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount = 0;

    @Column(name = "metric_before")
    private Double metricBefore;

    @Column(name = "metric_after")
    private Double metricAfter;

    @Column(name = "version", nullable = false)
    private Integer version = 1;
}
