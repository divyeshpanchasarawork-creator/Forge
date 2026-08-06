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
        repository.save(row);
        cached = weights;
        log.info("Calibration applied new scorer weights (v{}) on {} samples: MSE {} -> {}",
                row.getVersion(), sampleCount, metricBefore, metricAfter);
    }

    @Transactional
    public void recordMetrics(int sampleCount, double metricBefore, double metricAfter) {
        ScorerWeights row = currentRow();
        if (row.getWeightsJson() == null) {
            row.setWeightsJson(serialize(SignalWeights.DEFAULT));
        }
        row.setSampleCount(sampleCount);
        row.setMetricBefore(metricBefore);
        row.setMetricAfter(metricAfter);
        repository.save(row);
        log.info("Calibration kept current weights on {} samples (MSE {} vs candidate {}); no swap",
                sampleCount, metricBefore, metricAfter);
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
