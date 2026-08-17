package com.forge.calibration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.calibration.entity.ScorerWeights;
import com.forge.calibration.repository.ScorerWeightsRepository;
import com.forge.common.util.SignalWeights;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * Owns the single global {@link SignalWeights} row. Exposes the active weight vector to
 * the request path (cached in memory) and persists calibrated weights + evaluation
 * metrics written by {@link CalibrationJob}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScorerWeightsService {

    private static final String PARSE_ERROR = "Failed to parse stored scorer weights; falling back to defaults";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ScorerWeightsRepository repository;

    private volatile SignalWeights cached;

    public SignalWeights currentWeights() {
        SignalWeights weights = cached;
        if (weights == null) {
            weights = loadWeights();
            cached = weights;
        }
        return weights;
    }

    @Transactional
    public void applyWeights(SignalWeights weights, int sampleCount, double metricBefore, double metricAfter) {
        ScorerWeights row = currentRow();
        row.setWeightsJson(serialize(weights));
        row.setSampleCount(sampleCount);
        row.setMetricBefore(metricBefore);
        row.setMetricAfter(metricAfter);
        row.setVersion(row.getVersion() == null ? 1 : row.getVersion() + 1);
        row.setLastCalibratedAt(LocalDateTime.now());
        repository.save(row);
        publishCacheUpdate(weights);
        log.info("Calibration applied new scorer weights (v{}) on {} samples: MSE {} -> {}",
                row.getVersion(), sampleCount, metricBefore, metricAfter);
    }

    /**
     * Publishes the new vector to the volatile cache only after the surrounding transaction
     * has committed. Writing it eagerly would leave the in-memory weights ahead of the DB if
     * the transaction later rolls back, silently desyncing scoring from the stored row.
     */
    private void publishCacheUpdate(SignalWeights weights) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cached = weights;
                }
            });
        } else {
            cached = weights;
        }
    }

    /**
     * Records the metrics of a run that did not swap weights. Never creates the weights row:
     * without an apply there is nothing to calibrate, and a row created here would make the
     * engine look calibrated (version = 1) when it never was.
     */
    @Transactional
    public void recordMetrics(int sampleCount, double metricBefore, double metricAfter) {
        repository.findFirstByOrderByCreatedAtDesc().ifPresent(row -> {
            row.setSampleCount(sampleCount);
            row.setMetricBefore(metricBefore);
            row.setMetricAfter(metricAfter);
            repository.save(row);
            log.info("Calibration kept current weights on {} samples (MSE {} vs candidate {}); no swap",
                    sampleCount, metricBefore, metricAfter);
        });
    }

    private ScorerWeights currentRow() {
        return repository.findFirstByOrderByCreatedAtDesc().orElseGet(ScorerWeights::new);
    }

    private SignalWeights loadWeights() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(row -> parse(row.getWeightsJson()))
                .orElse(SignalWeights.DEFAULT);
    }

    private String serialize(SignalWeights weights) {
        try {
            return OBJECT_MAPPER.writeValueAsString(weights.toArray());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize scorer weights", e);
        }
    }

    private SignalWeights parse(String json) {
        try {
            return SignalWeights.from(OBJECT_MAPPER.readValue(json, double[].class));
        } catch (Exception e) {
            log.warn(PARSE_ERROR, e);
            return SignalWeights.DEFAULT;
        }
    }
}
