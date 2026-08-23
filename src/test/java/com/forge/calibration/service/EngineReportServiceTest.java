package com.forge.calibration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.calibration.dto.EngineReport;
import com.forge.calibration.entity.ScorerWeights;
import com.forge.calibration.repository.ScorerWeightsRepository;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SignalWeights;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngineReportServiceTest {

    private static final int SIGNALS = SignalWeights.SIGNAL_NAMES.size();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private ProblemAttemptRepository attemptRepository;
    @Mock private ScorerWeightsRepository scorerWeightsRepository;
    @Mock private ScorerWeightsService scorerWeightsService;

    @Test
    void shouldReportStoredVsLiveMetrics() throws Exception {
        List<ProblemAttempt> attempts = new ArrayList<>();
        attempts.add(attempt(signalsAt(0, 100), 15, 5));
        attempts.add(attempt(signalsAt(1, 100), 12, 5));
        attempts.add(attempt(new double[SIGNALS], 0, 0));

        SignalWeights weights = SignalWeights.from(new double[]{0.9, 0.9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        ScorerWeights row = new ScorerWeights();
        row.setVersion(3);
        row.setMetricBefore(5000.0);
        row.setMetricAfter(60.0);
        row.setLastCalibratedAt(LocalDateTime.of(2026, 8, 6, 2, 0));

        when(attemptRepository.findWithPredictedScores(any())).thenReturn(attempts);
        when(scorerWeightsService.currentWeights()).thenReturn(weights);
        when(scorerWeightsRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(java.util.Optional.of(row));

        EngineReport report = new EngineReportService(attemptRepository, scorerWeightsRepository, scorerWeightsService).getReport();

        assertEquals(3, report.sampleCount());
        assertEquals(CalibrationJob.minRequiredSamples(), report.minSamples());
        assertEquals(14969.0 / 3.0, report.storedMse(), 1e-9);
        assertEquals(1.0, report.storedAuc(), 1e-9);
        // Live metrics are leave-one-out: each sample is predicted by a fit on the other two.
        // The held-out sample's only active signal is absent from its training set, so all
        // three LOO predictions are 0 — an honest signal that 2-sample fits cannot generalize.
        assertEquals(20000.0 / 3.0, report.liveMse(), 1e-6);
        assertEquals(0.5, report.liveAuc(), 1e-9);
        assertEquals(2 * -Math.log(1e-9) / 3.0, report.liveLogLoss(), 1e-9);
        assertEquals(weights, report.weights());
        assertEquals(3, report.version());
        assertEquals(5000.0, report.lastMetricBefore());
        assertEquals(60.0, report.lastMetricAfter());
        assertEquals(row.getLastCalibratedAt(), report.lastCalibratedAt());
    }

    @Test
    void shouldReportNullCalibratedAtWhenWeightsNeverApplied() throws Exception {
        List<ProblemAttempt> attempts = new ArrayList<>();
        attempts.add(attempt(signalsAt(0, 100), 15, 5));

        SignalWeights weights = SignalWeights.DEFAULT;
        ScorerWeights row = new ScorerWeights();
        row.setVersion(1);
        row.setLastCalibratedAt(null);

        when(attemptRepository.findWithPredictedScores(any())).thenReturn(attempts);
        when(scorerWeightsService.currentWeights()).thenReturn(weights);
        when(scorerWeightsRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(java.util.Optional.of(row));

        EngineReport report = new EngineReportService(attemptRepository, scorerWeightsRepository, scorerWeightsService).getReport();

        assertNull(report.lastCalibratedAt(),
                "a metrics-only row must not look calibrated");
    }

    @Test
    void shouldReturnEmptyReportWhenNoSamples() {
        when(attemptRepository.findWithPredictedScores(any())).thenReturn(List.of());
        when(scorerWeightsService.currentWeights()).thenReturn(SignalWeights.DEFAULT);

        EngineReport report = new EngineReportService(attemptRepository, scorerWeightsRepository, scorerWeightsService).getReport();

        assertEquals(0, report.sampleCount());
        assertEquals(CalibrationJob.minRequiredSamples(), report.minSamples());
        assertTrue(Double.isNaN(report.liveMse()));
    }

    private double[] signalsAt(int index, double value) {
        double[] signals = new double[SIGNALS];
        signals[index] = value;
        return signals;
    }

    private ProblemAttempt attempt(double[] signals, int predictedScore, int quality) throws Exception {
        ProblemAttempt a = new ProblemAttempt();
        a.setId(UUID.randomUUID());
        List<ProblemScorer.ScoreItem> items = new ArrayList<>();
        for (int i = 0; i < signals.length; i++) {
            items.add(new ProblemScorer.ScoreItem(SignalWeights.SIGNAL_NAMES.get(i), 0.1,
                    (int) Math.round(signals[i]), 0));
        }
        a.setSignalsJson(objectMapper.writeValueAsString(items));
        a.setPredictedScore(predictedScore);
        a.setQuality(quality);
        return a;
    }
}
