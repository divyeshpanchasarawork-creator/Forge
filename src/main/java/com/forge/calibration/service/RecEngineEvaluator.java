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
    /** Ridge expressed as a fraction of the mean normal-equation diagonal, so it stays
     *  meaningful regardless of the signal scale (raw signals are 0-100). */
    public static final double RIDGE_RATIO = 1e-3;

    /**
     * Least-squares fit mapping signal vectors to reward*100 (reward on 0-1, quality/5),
     * ridge-regularized and clamped to [0, 0.5]. Returns null on empty/mismatched input.
     */
    public static double[] fitLeastSquares(List<double[]> xs, double[] rewards) {
        if (xs == null || rewards == null || xs.isEmpty() || xs.size() != rewards.length) return null;
        int k = xs.get(0).length;
        if (k == 0) return null;
        double[][] a = new double[k][k];
        double[] b = new double[k];
        for (int n = 0; n < xs.size(); n++) {
            double[] x = xs.get(n);
            if (x.length != k) return null;
            double y = rewards[n] * 100.0;
            for (int i = 0; i < k; i++) {
                b[i] += x[i] * y;
                for (int j = 0; j < k; j++) {
                    a[i][j] += x[i] * x[j];
                }
            }
        }
        addRidge(a, k);
        double[] w = solve(a, b);
        for (int i = 0; i < k; i++) {
            if (!Double.isFinite(w[i])) {
                w[i] = 0;
            }
            w[i] = Math.max(0, Math.min(0.5, w[i]));
        }
        return w;
    }

    /**
     * Leave-one-out cross-validated predictions: for each sample, fit on the other n-1 and
     * predict the held-out one. This is the honest estimate of how the fit generalizes — a
     * fit evaluated on the same samples it was trained on always looks better than it is.
     * Returns null when the set is too small to fit a holdout model.
     */
    public static List<Double> cvPredictions(List<double[]> xs, double[] rewards) {
        if (xs == null || rewards == null || xs.size() < 2 || xs.size() != rewards.length) return null;
        List<Double> preds = new ArrayList<>(xs.size());
        for (int i = 0; i < xs.size(); i++) {
            List<double[]> train = new ArrayList<>(xs.size() - 1);
            double[] trainRewards = new double[xs.size() - 1];
            int t = 0;
            for (int j = 0; j < xs.size(); j++) {
                if (j == i) continue;
                train.add(xs.get(j));
                trainRewards[t++] = rewards[j];
            }
            double[] w = fitLeastSquares(train, trainRewards);
            if (w == null) return null;
            preds.add((double) predict(w, xs.get(i)));
        }
        return preds;
    }

    /**
     * Leave-one-out cross-validated metrics. NaN when the set is too small to hold out.
     * Use these instead of in-sample metrics whenever the weights were fit on the same data.
     */
    public static CvMetrics cvMetrics(List<double[]> xs, double[] rewards) {
        List<Double> preds = cvPredictions(xs, rewards);
        if (preds == null) {
            return new CvMetrics(Double.NaN, Double.NaN, Double.NaN);
        }
        List<Double> actual = new ArrayList<>(rewards.length);
        for (double r : rewards) {
            actual.add(r);
        }
        return new CvMetrics(mse(preds, actual), logLoss(preds, actual), auc(preds, actual));
    }

    public record CvMetrics(double mse, double logLoss, double auc) {}

    /** Predicted total score (0-100) from a weight vector and per-signal values (0-100). */
    public static int predict(double[] weights, double[] signals) {
        double sum = 0;
        for (int j = 0; j < weights.length; j++) {
            sum += weights[j] * signals[j];
        }
        return (int) Math.round(Math.min(100, sum));
    }

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

    private static void addRidge(double[][] a, int k) {
        double meanDiag = 0;
        for (int i = 0; i < k; i++) {
            meanDiag += a[i][i];
        }
        meanDiag /= k;
        double ridge = RIDGE_RATIO * meanDiag;
        for (int i = 0; i < k; i++) {
            a[i][i] += ridge;
        }
    }

    /** Solves a*x = b via Gaussian elimination with partial pivoting. */
    private static double[] solve(double[][] a, double[] b) {
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

    private record Score(double score, boolean positive) {}
}
