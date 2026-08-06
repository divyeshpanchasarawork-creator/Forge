package com.forge.calibration.service;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure evaluation metrics for the recommendation engine. Calibration and the engine
 * health report share these so that "metric improves" always means the same thing.
 * Predicted values are expected on the score's 0-100 scale; rewards on 0-1 (quality/5).
 */
@UtilityClass
public class RecEngineEvaluator {

    public static final double SUCCESS_REWARD = 0.6;

    /** Mean squared error between predicted (0-100) and actual reward scaled to 0-100. */
    public static double mse(List<Double> predicted, List<Double> actual) {
        if (predicted.isEmpty()) return Double.NaN;
        double sum = 0;
        for (int i = 0; i < predicted.size(); i++) {
            double err = predicted.get(i) - actual.get(i) * 100.0;
            sum += err * err;
        }
        return sum / predicted.size();
    }

    /** Binary log-loss, treating reward &gt;= {@value #SUCCESS_REWARD} as a success. Predicted 0-100 -> probability. */
    public static double logLoss(List<Double> predicted, List<Double> actual) {
        if (predicted.isEmpty()) return Double.NaN;
        double sum = 0;
        for (int i = 0; i < predicted.size(); i++) {
            boolean positive = actual.get(i) >= SUCCESS_REWARD;
            double p = Math.max(1e-9, Math.min(1 - 1e-9, predicted.get(i) / 100.0));
            sum += positive ? -Math.log(p) : -Math.log(1 - p);
        }
        return sum / predicted.size();
    }

    /**
     * Rank-AUC (Mann-Whitney) between predicted score and binary success, with average-rank
     * tie handling. 1.0 = perfect ranking, 0.5 = random, 0.0 = perfectly inverted.
     */
    public static double auc(List<Double> predicted, List<Double> actual) {
        if (predicted.isEmpty()) return Double.NaN;
        List<Score> scores = new ArrayList<>(predicted.size());
        for (int i = 0; i < predicted.size(); i++) {
            scores.add(new Score(predicted.get(i), actual.get(i) >= SUCCESS_REWARD));
        }
        scores.sort(Comparator.comparingDouble(Score::score));

        int positives = (int) scores.stream().filter(Score::positive).count();
        int negatives = scores.size() - positives;
        if (positives == 0 || negatives == 0) return Double.NaN;

        double rankSum = 0;
        int i = 0;
        while (i < scores.size()) {
            int j = i;
            while (j + 1 < scores.size() && Double.compare(scores.get(j + 1).score(), scores.get(i).score()) == 0) {
                j++;
            }
            double avgRank = (i + 1 + j + 1) / 2.0;
            for (int k = i; k <= j; k++) {
                if (scores.get(k).positive()) {
                    rankSum += avgRank;
                }
            }
            i = j + 1;
        }

        double u = rankSum - (long) positives * (positives + 1) / 2.0;
        return u / ((double) positives * negatives);
    }

    private record Score(double score, boolean positive) {}
}
