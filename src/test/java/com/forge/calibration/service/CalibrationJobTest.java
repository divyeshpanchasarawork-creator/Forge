package com.forge.calibration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.calibration.dto.CalibrationResult;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SignalWeights;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalibrationJobTest {

    private static final int SIGNALS = SignalWeights.SIGNAL_NAMES.size();
    private static final int MIN_SAMPLES = 10;
    private static final int MIN_REQUIRED_SAMPLES = CalibrationJob.minRequiredSamples();

    @Mock private ProblemAttemptRepository attemptRepository;
    @Mock private ScorerWeightsService scorerWeightsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CalibrationJob job;

    @BeforeEach
    void setUp() {
        job = new CalibrationJob(attemptRepository, scorerWeightsService);
    }

    @Test
    void shouldSkipWhenFewerThanTenSamples() {
        when(attemptRepository.findWithPredictedScores(any())).thenReturn(List.of());

        CalibrationResult result = job.calibrate();

        assertEquals("SKIPPED", result.status());
        assertFalse(result.applied());
        assertEquals(0, result.sampleCount());
        assertEquals(MIN_REQUIRED_SAMPLES, result.minSamples());
        verify(scorerWeightsService, never()).applyWeights(any(), anyInt(), anyDouble(), anyDouble());
        verify(scorerWeightsService, never()).recordMetrics(anyInt(), anyDouble(), anyDouble());
    }

    @Test
    void shouldSwapWeightsWhenMseImproves() throws Exception {
        List<ProblemAttempt> attempts = new ArrayList<>();
        List<double[]> signals = new ArrayList<>();
        List<Integer> qualities = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double x0 = (i * 37) % 101;
            double x1 = (i * 53) % 101;
            double target = 0.4 * x0 + 0.3 * x1;
            double[] s = new double[SIGNALS];
            s[0] = x0;
            s[1] = x1;
            int quality = Math.max(0, Math.min(5, (int) Math.round(target / 20)));
            signals.add(s);
            qualities.add(quality);
            attempts.add(attempt(s, quality));
        }
        when(attemptRepository.findWithPredictedScores(any())).thenReturn(attempts);
        when(scorerWeightsService.currentWeights()).thenReturn(SignalWeights.DEFAULT);

        CalibrationResult result = job.calibrate();

        ArgumentCaptor<SignalWeights> weightsCaptor = ArgumentCaptor.forClass(SignalWeights.class);
        ArgumentCaptor<Double> beforeCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> afterCaptor = ArgumentCaptor.forClass(Double.class);
        verify(scorerWeightsService).applyWeights(weightsCaptor.capture(), eq(48), beforeCaptor.capture(), afterCaptor.capture());

        assertTrue(result.applied(), "weights should be applied when MSE improves");
        assertEquals("APPLIED", result.status());
        assertTrue(afterCaptor.getValue() < beforeCaptor.getValue(),
                "MSE should drop after calibration, got " + beforeCaptor.getValue() + " -> " + afterCaptor.getValue());
        assertTrue(mse(SignalWeights.DEFAULT, signals, qualities) > mse(weightsCaptor.getValue(), signals, qualities),
                "fitted weights should beat the default vector on stored samples");
    }

    @Test
    void shouldKeepWeightsWhenNoMeaningfulImprovement() throws Exception {
        List<ProblemAttempt> attempts = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            attempts.add(attempt(new double[SIGNALS], 0));
        }
        when(attemptRepository.findWithPredictedScores(any())).thenReturn(attempts);
        when(scorerWeightsService.currentWeights()).thenReturn(SignalWeights.DEFAULT);

        CalibrationResult result = job.calibrate();

        assertFalse(result.applied(), "no swap when improvement is below threshold");
        assertEquals("SKIPPED", result.status());
        verify(scorerWeightsService, never()).applyWeights(any(), anyInt(), anyDouble(), anyDouble());
        verify(scorerWeightsService).recordMetrics(40, 0.0, 0.0);
    }

    @Test
    void shouldRejectCandidateThatRanksAtOrBelowRandom() throws Exception {
        // Quality is inversely related to the signal: high x0 -> low quality. The fitted weights
        // (clipped non-negative) predict higher scores for higher x0, so the candidate ranks the
        // samples backwards and its AUC is at or below random. It must not be swapped in even if
        // its in-sample MSE improved.
        List<ProblemAttempt> attempts = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            double x0 = (i * 37) % 101;
            double[] s = new double[SIGNALS];
            s[0] = x0;
            int quality = (x0 >= 50) ? 0 : 5;
            attempts.add(attempt(s, quality));
        }
        when(attemptRepository.findWithPredictedScores(any())).thenReturn(attempts);
        when(scorerWeightsService.currentWeights()).thenReturn(SignalWeights.DEFAULT);

        CalibrationResult result = job.calibrate();

        verify(scorerWeightsService, never()).applyWeights(any(), anyInt(), anyDouble(), anyDouble());
        assertFalse(result.applied(), "candidate with degenerate AUC must not be swapped in");
    }

    @Test
    void shouldRejectCandidateThatFitsTrainingButRanksBackwardsOnHoldout() throws Exception {
        // Samples arrive newest-first: indices 0-11 are the held-out validation slice, the rest
        // are training. Training quality tracks x0 positively; holdout quality anti-correlates.
        // A candidate fit on training predicts high scores for high x0, so on the untouched
        // holdout it ranks backwards and must be rejected.
        List<ProblemAttempt> attempts = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double x0 = (i * 37) % 101;
            double[] s = new double[SIGNALS];
            s[0] = x0;
            boolean holdout = i < 12;
            int quality = holdout ? ((x0 >= 50) ? 0 : 5) : ((x0 >= 50) ? 5 : 0);
            attempts.add(attempt(s, quality));
        }
        when(attemptRepository.findWithPredictedScores(any())).thenReturn(attempts);
        when(scorerWeightsService.currentWeights()).thenReturn(SignalWeights.DEFAULT);

        CalibrationResult result = job.calibrate();

        verify(scorerWeightsService, never()).applyWeights(any(), anyInt(), anyDouble(), anyDouble());
        assertFalse(result.applied(), "candidate that inverts holdout ranking must not be swapped in");
    }

    private ProblemAttempt attempt(double[] signals, int quality) throws Exception {
        ProblemAttempt a = new ProblemAttempt();
        a.setId(UUID.randomUUID());
        List<ProblemScorer.ScoreItem> items = new ArrayList<>();
        for (int i = 0; i < signals.length; i++) {
            items.add(new ProblemScorer.ScoreItem(SignalWeights.SIGNAL_NAMES.get(i), 0.1,
                    (int) Math.round(signals[i]), 0));
        }
        a.setSignalsJson(objectMapper.writeValueAsString(items));
        a.setQuality(quality);
        a.setPredictedScore(0);
        return a;
    }

    private double mse(SignalWeights weights, List<double[]> signals, List<Integer> qualities) {
        double[] w = weights.toArray();
        double sum = 0;
        for (int i = 0; i < signals.size(); i++) {
            double predicted = 0;
            double[] s = signals.get(i);
            for (int j = 0; j < w.length; j++) {
                predicted += w[j] * s[j];
            }
            double actual = qualities.get(i) * 20.0;
            double err = Math.min(100, predicted) - actual;
            sum += err * err;
        }
        return sum / signals.size();
    }
}
