package com.forge.calibration.dto;

import com.forge.common.util.SignalWeights;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Engine health report: stored-vs-live prediction metrics over the attempt snapshots,
 * the active weight vector, the last calibration outcome, and the recent nightly-run
 * history (newest first) for trend display.
 */
public record EngineReport(
        int sampleCount,
        int minSamples,
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
        LocalDateTime lastCalibratedAt,
        List<RunView> recentRuns) {

    /**
     * One ledger row of {@link com.forge.calibration.entity.CalibrationRun}, shaped for
     * rendering: when it ran, whether weights were swapped, and the holdout MSE before/after.
     */
    public record RunView(
            LocalDateTime ranAt,
            String status,
            int sampleCount,
            Double metricBefore,
            Double metricAfter,
            boolean swapped,
            String message) {
    }
}
