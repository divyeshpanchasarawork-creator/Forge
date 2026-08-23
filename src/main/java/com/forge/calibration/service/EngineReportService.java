package com.forge.calibration.service;

import com.forge.calibration.dto.EngineReport;
import com.forge.calibration.entity.ScorerWeights;
import com.forge.calibration.repository.ScorerWeightsRepository;
import com.forge.common.util.SignalWeights;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EngineReportService {

    private static final int MAX_SAMPLES = 300;

    private final ProblemAttemptRepository attemptRepository;
    private final ScorerWeightsRepository scorerWeightsRepository;
    private final ScorerWeightsService scorerWeightsService;

    @Transactional(readOnly = true)
    public EngineReport getReport() {
        List<ProblemAttempt> attempts = attemptRepository.findWithPredictedScores(PageRequest.of(0, MAX_SAMPLES));
        List<Sample> samples = new ArrayList<>();
        for (ProblemAttempt attempt : attempts) {
            if (attempt.getSignalsJson() == null || attempt.getQuality() == null || attempt.getPredictedScore() == null) {
                continue;
            }
            double[] signals = CalibrationJob.parseSignals(attempt.getSignalsJson());
            if (signals == null) {
                continue;
            }
            samples.add(new Sample(signals, attempt.getQuality() / 5.0, attempt.getPredictedScore()));
        }

        if (samples.isEmpty()) {
            return new EngineReport(0, CalibrationJob.minRequiredSamples(), Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN,
                    scorerWeightsService.currentWeights(), null, null, null, null);
        }

        SignalWeights weights = scorerWeightsService.currentWeights();
        List<Double> stored = new ArrayList<>();
        List<double[]> xs = new ArrayList<>();
        List<Double> actual = new ArrayList<>();
        for (Sample s : samples) {
            stored.add((double) s.predicted());
            xs.add(s.signals());
            actual.add(s.reward());
        }
        RecEngineEvaluator.CvMetrics live = RecEngineEvaluator.cvMetrics(xs, toArray(actual));

        ScorerWeights row = scorerWeightsRepository.findFirstByOrderByCreatedAtDesc().orElse(null);
        Integer version = row != null ? row.getVersion() : null;
        Double before = row != null ? row.getMetricBefore() : null;
        Double after = row != null ? row.getMetricAfter() : null;
        java.time.LocalDateTime calibratedAt = row != null ? row.getLastCalibratedAt() : null;

        return new EngineReport(
                samples.size(),
                CalibrationJob.minRequiredSamples(),
                RecEngineEvaluator.mse(stored, actual),
                RecEngineEvaluator.logLoss(stored, actual),
                RecEngineEvaluator.auc(stored, actual),
                live.mse(),
                live.logLoss(),
                live.auc(),
                weights,
                version,
                before,
                after,
                calibratedAt);
    }

    private static double[] toArray(List<Double> values) {
        double[] out = new double[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private record Sample(double[] signals, double reward, int predicted) {}
}
