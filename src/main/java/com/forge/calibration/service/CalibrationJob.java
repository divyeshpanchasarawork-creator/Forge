package com.forge.calibration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SignalWeights;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Nightly recalibration of the scorer weights. Re-scores stored signal snapshots under the
 * current and a least-squares fitted weight vector, and swaps only when MSE improves by at
 * least the threshold on at least {@value #MIN_SAMPLES} samples.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalibrationJob {

    private static final int MIN_SAMPLES = 30;
    private static final int MAX_SAMPLES = 300;
    private static final double RIDGE = 1e-6;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProblemAttemptRepository attemptRepository;
    private final ScorerWeightsService scorerWeightsService;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void calibrate() {
        List<ProblemAttempt> attempts = attemptRepository.findWithPredictedScores(PageRequest.of(0, MAX_SAMPLES));
        List<SignalSample> samples = attempts.stream()
                .filter(a -> a.getSignalsJson() != null && a.getQuality() != null)
                .map(this::parseSample)
                .filter(Objects::nonNull)
                .toList();

        if (samples.size() < MIN_SAMPLES) {
            log.info("Calibration skipped: {} samples available (need {})", samples.size(), MIN_SAMPLES);
            return;
        }

        SignalWeights current = scorerWeightsService.currentWeights();
        double before = evaluate(current, samples);
        SignalWeights candidate = fit(samples);
        double after = evaluate(candidate, samples);

        double threshold = Math.max(1.0, 0.05 * before);
        if (before - after >= threshold) {
            scorerWeightsService.applyWeights(candidate, samples.size(), before, after);
        } else {
            scorerWeightsService.recordMetrics(samples.size(), before, after);
        }
    }

    private double evaluate(SignalWeights weights, List<SignalSample> samples) {
        List<Double> predicted = samples.stream()
                .map(s -> (double) RecEngineEvaluator.predict(weights.toArray(), s.signals()))
                .toList();
        List<Double> actual = samples.stream().map(SignalSample::reward).toList();
        return RecEngineEvaluator.mse(predicted, actual);
    }

    /**
     * Parses the stored signal snapshot (the {@code ScoreItem} list) back into per-signal
     * values aligned with {@link SignalWeights#SIGNAL_NAMES}; returns null when unparseable.
     */
    public static double[] parseSignals(String signalsJson) {
        try {
            List<ProblemScorer.ScoreItem> items = OBJECT_MAPPER.readValue(
                    signalsJson, new TypeReference<List<ProblemScorer.ScoreItem>>() {});
            double[] signals = new double[SignalWeights.SIGNAL_NAMES.size()];
            for (ProblemScorer.ScoreItem item : items) {
                int idx = SignalWeights.SIGNAL_NAMES.indexOf(item.name());
                if (idx >= 0) {
                    signals[idx] = item.value();
                }
            }
            return signals;
        } catch (Exception e) {
            return null;
        }
    }

    private SignalSample parseSample(ProblemAttempt attempt) {
        double[] signals = parseSignals(attempt.getSignalsJson());
        if (signals == null) {
            log.warn("Skipping attempt {} with unparseable signal snapshot", attempt.getId());
            return null;
        }
        return new SignalSample(signals, attempt.getQuality() / 5.0);
    }

    private SignalWeights fit(List<SignalSample> samples) {
        int k = SignalWeights.SIGNAL_NAMES.size();
        double[][] a = new double[k][k];
        double[] b = new double[k];
        for (SignalSample s : samples) {
            double[] x = s.signals();
            double y = s.reward() * 100.0;
            for (int i = 0; i < k; i++) {
                b[i] += x[i] * y;
                for (int j = 0; j < k; j++) {
                    a[i][j] += x[i] * x[j];
                }
            }
        }
        for (int i = 0; i < k; i++) {
            a[i][i] += RIDGE;
        }

        double[] w = solve(a, b);
        for (int i = 0; i < k; i++) {
            if (!Double.isFinite(w[i])) {
                w[i] = 0;
            }
            w[i] = Math.max(0, Math.min(0.5, w[i]));
        }
        return SignalWeights.from(w);
    }

    /** Solves a*x = b via Gaussian elimination with partial pivoting. */
    private double[] solve(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(m[pivot][col]) < 1e-12) {
                continue;
            }
            double[] tmp = m[col];
            m[col] = m[pivot];
            m[pivot] = tmp;
            for (int row = col + 1; row < n; row++) {
                double factor = m[row][col] / m[col][col];
                for (int j = col; j <= n; j++) {
                    m[row][j] -= factor * m[col][j];
                }
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = m[i][n];
            for (int j = i + 1; j < n; j++) {
                sum -= m[i][j] * x[j];
            }
            x[i] = Math.abs(m[i][i]) < 1e-12 ? 0 : sum / m[i][i];
        }
        return x;
    }

    private record SignalSample(double[] signals, double reward) {}
}
