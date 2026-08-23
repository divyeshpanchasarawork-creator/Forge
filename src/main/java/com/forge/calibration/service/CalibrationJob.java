package com.forge.calibration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.calibration.dto.CalibrationResult;
import com.forge.calibration.entity.CalibrationRun;
import com.forge.calibration.repository.CalibrationRunRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Nightly recalibration of the scorer weights. Fits a least-squares weight vector over stored
 * signal snapshots and swaps it in only when it beats the incumbent on a temporally held-out
 * validation slice (the newest samples) across at least {@value #MIN_SAMPLES} training samples —
 * in-sample metrics are gamed by underdetermined fits, and scoring both vectors on the same
 * untouched slice keeps the swap comparison symmetric.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalibrationJob {

    public static final int MIN_SAMPLES = 10;
    /** Newest samples held back to validate both weight vectors on equal footing. */
    public static final int MIN_HOLDOUT = 2;
    private static final int MAX_SAMPLES = 300;
    /** A candidate predictor that ranks at or below random is never swapped in. */
    private static final double MIN_AUC = 0.5;

    public static int minRequiredSamples() {
        return MIN_SAMPLES + MIN_HOLDOUT;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProblemAttemptRepository attemptRepository;
    private final ScorerWeightsService scorerWeightsService;
    private final CalibrationRunRepository calibrationRunRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    @Transactional
    public CalibrationResult calibrate() {
        LocalDateTime ranAt = LocalDateTime.now();
        List<ProblemAttempt> attempts = attemptRepository.findWithPredictedScores(PageRequest.of(0, MAX_SAMPLES));
        List<SignalSample> samples = attempts.stream()
                .filter(a -> a.getSignalsJson() != null && a.getQuality() != null)
                .map(this::parseSample)
                .filter(Objects::nonNull)
                .toList();

        if (samples.size() < minRequiredSamples()) {
            String message = "Calibration skipped: " + samples.size() + " of " + minRequiredSamples()
                    + " minimum scored samples available. Practice more to grow the calibration set.";
            log.info(message);
            return record(ranAt, CalibrationResult.skipped(samples.size(), minRequiredSamples(), message));
        }

        // Temporal holdout: samples are newest-first. Fit the candidate on the older majority
        // and score BOTH vectors on the newest slice — the candidate cannot memorize what it
        // is judged on, and incumbent vs candidate face an identical evaluation.
        int holdoutSize = Math.max(MIN_HOLDOUT, samples.size() / 5);
        List<SignalSample> validation = samples.subList(0, holdoutSize);
        List<SignalSample> training = samples.subList(holdoutSize, samples.size());

        SignalWeights current = scorerWeightsService.currentWeights();
        double before = evaluate(current, validation);

        SignalWeights candidate;
        try {
            candidate = SignalWeights.from(RecEngineEvaluator.fitLeastSquares(xsOf(training), rewardsOf(training)));
        } catch (Exception ex) {
            String message = "Calibration skipped: fit failed (" + ex.getMessage() + "). Keeping current weights.";
            log.warn("Calibration fit failed: {}", ex.getMessage());
            return record(ranAt, CalibrationResult.skipped(samples.size(), minRequiredSamples(), message));
        }
        double after = evaluate(candidate, validation);

        if (shouldApply(before, after, aucOf(candidate, validation), aucOf(current, validation))) {
            scorerWeightsService.applyWeights(candidate, training.size(), before, after);
            String message = String.format(
                    "Calibration applied: holdout MSE %.2f -> %.2f (%d training / %d held-out samples). New weights active.",
                    before, after, training.size(), validation.size());
            log.info(message);
            return record(ranAt, CalibrationResult.applied(training.size(), minRequiredSamples(), before, after, message));
        } else {
            scorerWeightsService.recordMetrics(samples.size(), before, after);
            String message = String.format(
                    "Calibration ran but kept current weights: holdout MSE %.2f -> %.2f on %d held-out samples did not clear the swap bar.",
                    before, after, validation.size());
            log.info(message);
            return record(ranAt, CalibrationResult.skipped(samples.size(), minRequiredSamples(), before, after, message));
        }
    }

    /**
     * Persists the outcome to the append-only {@link CalibrationRun} ledger so the engine
     * health card can chart a trend; runs within the surrounding transaction of calibrate().
     */
    private CalibrationResult record(LocalDateTime ranAt, CalibrationResult result) {
        CalibrationRun run = new CalibrationRun();
        run.setRanAt(ranAt);
        run.setStatus(result.status());
        run.setSampleCount(result.sampleCount());
        run.setMinSamples(result.minSamples());
        run.setMetricBefore(result.before());
        run.setMetricAfter(result.after());
        run.setSwapped(result.applied());
        run.setMessage(result.message());
        calibrationRunRepository.save(run);
        return result;
    }

    /**
     * Swap the candidate weights only when its holdout MSE improvement over the incumbent clears
     * the threshold AND it does not rank at-or-below random or meaningfully worse than the
     * incumbent on the same held-out samples.
     */
    private boolean shouldApply(double before, double after, double candidateAuc, double currentAuc) {
        if (!Double.isFinite(after)) return false;
        double threshold = Math.max(1.0, 0.05 * before);
        if (before - after < threshold) return false;

        if (Double.isFinite(candidateAuc) && candidateAuc <= MIN_AUC) return false;
        if (Double.isFinite(candidateAuc) && Double.isFinite(currentAuc)
                && candidateAuc + 0.01 < currentAuc) return false;
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
