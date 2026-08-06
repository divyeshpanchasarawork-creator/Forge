package com.forge.calibration.dto;

import com.forge.common.util.SignalWeights;

import java.time.LocalDateTime;

/**
 * Engine health report: stored-vs-live prediction metrics over the attempt snapshots, plus
 * the active weight vector and the last calibration outcome.
 */
public record EngineReport(
        int sampleCount,
        double storedMse,
        double storedLogLoss,
        double storedAuc,
        double liveMse,
        double liveLogLoss,
        double liveAuc,
        SignalWeights weights,
        Integer version,
        Double lastMetricBefore,
        Double lastMetricAfter,
        LocalDateTime lastCalibratedAt) {
}
