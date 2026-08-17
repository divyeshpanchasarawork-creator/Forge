package com.forge.calibration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.calibration.dto.CalibrationResult;
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
 * Nightly recalibration of the scorer weights. Fits a least-squares weight vector over stored
 * signal snapshots and swaps it in only when its leave-one-out metrics clear the bar on at least
 * {@value #MIN_SAMPLES} samples — in-sample metrics are gamed by underdetermined fits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalibrationJob {

    public static final int MIN_SAMPLES = 10;
    private static final int MAX_SAMPLES = 300;
    /** A candidate predictor that ranks at or below random is never swapped in. */
    private static final double MIN_AUC = 0.5;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProblemAttemptRepository attemptRepository;
    private final ScorerWeightsService scorerWeightsService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    @Transactional
    public CalibrationResult calibrate() {
        List<ProblemAttempt> attempts = attemptRepository.findWithPredictedScores(PageRequest.of(0, MAX_SAMPLES));
        List<SignalSample> samples = attempts.stream()
                .filter(a -> a.getSignalsJson() != null && a.getQuality() != null)
                .map(this::parseSample)
                .filter(Objects::nonNull)
                .toList();

        if (samples.size() < MIN_SAMPLES) {
            String message = "Calibration skipped: " + samples.size() + " of " + MIN_SAMPLES
                    + " minimum scored samples available. Practice more to grow the calibration set.";
            log.info(message);
            return CalibrationResult.skipped(samples.size(), MIN_SAMPLES, message);
        }

        SignalWeights current = scorerWeightsService.currentWeights();
        double before = evaluate(current, samples);
        SignalWeights candidate;
        try {
            candidate = SignalWeights.from(RecEngineEvaluator.fitLeastSquares(xsOf(samples), rewardsOf(samples)));
        } catch (Exception ex) {
            String message = "Calibration skipped: fit failed (" + ex.getMessage() + "). Keeping current weights.";
            log.warn("Calibration fit failed: {}", ex.getMessage());
            return CalibrationResult.skipped(samples.size(), MIN_SAMPLES, message);
        }
        RecEngineEvaluator.CvMetrics cv = RecEngineEvaluator.cvMetrics(xsOf(samples), rewardsOf(samples));
        double after = cv.mse();

        if (shouldApply(current, samples, before, cv)) {
            scorerWeightsService.applyWeights(candidate, samples.size(), before, after);
            String message = String.format(
                    "Calibration applied: MSE %.2f -> %.2f on %d samples. New weights active.",
                    before, after, samples.size());
            log.info(message);
            return CalibrationResult.applied(samples.size(), MIN_SAMPLES, before, after, message);
        } else {
            scorerWeightsService.recordMetrics(samples.size(), before, after);
            String message = String.format(
                    "Calibration ran but kept current weights: MSE %.2f -> %.2f on %d samples did not clear the swap bar.",
                    before, after, samples.size());
            log.info(message);
            return CalibrationResult.skipped(samples.size(), MIN_SAMPLES, before, after, message);
        }
    }

    /**
     * Swap the candidate weights only when its leave-one-out MSE improvement clears the threshold
     * AND its leave-one-out AUC does not rank worse than random (or worse than the current weights).
     * The LOO metrics are honest — a fit evaluated on the same samples it memorized always clears an
     * in-sample bar, which is exactly the degenerate case this guards against.
     */
    private boolean shouldApply(SignalWeights current, List<SignalSample> samples,
                                double before, RecEngineEvaluator.CvMetrics cv) {
        double threshold = Math.max(1.0, 0.05 * before);
        if (before - cv.mse() < threshold) return false;
        if (!Double.isFinite(cv.mse())) return false;

        if (Double.isFinite(cv.auc()) && cv.auc() <= MIN_AUC) return false;
        double currentAuc = aucOf(current, samples);
        if (Double.isFinite(currentAuc) && Double.isFinite(cv.auc())
                && cv.auc() + 0.01 < currentAuc) return false;
        return true;
    }

    private double evaluate(SignalWeights weights, List<SignalSample> samples) {
        List<Double> predicted = samples.stream()
                .map(s -> (double) RecEngineEvaluator.predict(weights.toArray(), s.signals()))
                .toList();
        List<Double> actual = samples.stream().map(SignalSample::reward).toList();
        return RecEngineEvaluator.mse(predicted, actual);
    }

    private double aucOf(SignalWeights weights, List<SignalSample> samples) {
        List<Double> predicted = samples.stream()
                .map(s -> (double) RecEngineEvaluator.predict(weights.toArray(), s.signals()))
                .toList();
        List<Double> actual = samples.stream().map(SignalSample::reward).toList();
        return RecEngineEvaluator.auc(predicted, actual);
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

    private static List<double[]> xsOf(List<SignalSample> samples) {
        return samples.stream().map(SignalSample::signals).toList();
    }

    private static double[] rewardsOf(List<SignalSample> samples) {
        return samples.stream().mapToDouble(SignalSample::reward).toArray();
    }

    private record SignalSample(double[] signals, double reward) {}
}
