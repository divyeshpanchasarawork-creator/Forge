package com.forge.calibration.service;

import com.forge.common.util.SignalWeights;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecEngineEvaluatorTest {

    @Test
    void mseShouldBeZeroForPerfectPredictions() {
        assertEquals(0.0, RecEngineEvaluator.mse(List.of(0.0, 50.0, 100.0), List.of(0.0, 0.5, 1.0)), 1e-9);
    }

    @Test
    void mseShouldUseZeroToHundredScale() {
        assertEquals(2500.0, RecEngineEvaluator.mse(List.of(50.0), List.of(0.0)), 1e-9);
    }

    @Test
    void logLossShouldBeLowForCalibratedPredictions() {
        List<Double> predicted = List.of(90.0, 10.0, 80.0, 20.0);
        List<Double> actual = List.of(1.0, 0.0, 0.6, 0.0);
        double loss = RecEngineEvaluator.logLoss(predicted, actual);
        assertTrue(loss < 0.2, "log-loss should be low, got " + loss);
    }

    @Test
    void logLossShouldBeHighForInvertedPredictions() {
        List<Double> predicted = List.of(10.0, 90.0);
        List<Double> actual = List.of(1.0, 0.0);
        double loss = RecEngineEvaluator.logLoss(predicted, actual);
        assertTrue(loss > 2.0, "log-loss should be high for inverted predictions, got " + loss);
    }

    @Test
    void aucShouldBeOneForPerfectRanking() {
        assertEquals(1.0, RecEngineEvaluator.auc(List.of(10.0, 20.0, 80.0, 90.0), List.of(0.0, 0.0, 1.0, 1.0)), 1e-9);
    }

    @Test
    void aucShouldBeZeroForInvertedRanking() {
        assertEquals(0.0, RecEngineEvaluator.auc(List.of(90.0, 80.0, 20.0, 10.0), List.of(0.0, 0.0, 1.0, 1.0)), 1e-9);
    }

    @Test
    void aucShouldBeHalfForCoinFlipRanking() {
        assertEquals(0.5, RecEngineEvaluator.auc(List.of(10.0, 20.0, 30.0, 40.0), List.of(1.0, 0.0, 0.0, 1.0)), 1e-9);
    }

    @Test
    void aucShouldBeHalfWhenAllPredictionsTie() {
        assertEquals(0.5, RecEngineEvaluator.auc(List.of(50.0, 50.0, 50.0), List.of(1.0, 1.0, 0.0)), 1e-9);
    }

    @Test
    void fitShouldRecoverLinearWeights() {
        List<double[]> xs = new ArrayList<>();
        double[] rewards = new double[40];
        for (int i = 0; i < 40; i++) {
            double x0 = (i * 37) % 101;
            double x1 = (i * 53) % 101;
            xs.add(new double[]{x0, x1});
            rewards[i] = (0.4 * x0 + 0.3 * x1) / 100.0;
        }

        double[] w = RecEngineEvaluator.fitLeastSquares(xs, rewards);

        assertEquals(0.4, w[0], 0.02);
        assertEquals(0.3, w[1], 0.02);
    }

    @Test
    void fitShouldClampToNonNegativeHalf() {
        List<double[]> xs = List.of(new double[]{100.0}, new double[]{0.0});
        assertEquals(0.0, RecEngineEvaluator.fitLeastSquares(xs, new double[]{0.0, 1.0})[0], 1e-9);
        assertEquals(0.5, RecEngineEvaluator.fitLeastSquares(xs, new double[]{1.0, 0.0})[0], 1e-9);
    }

    @Test
    void fitShouldReturnNullWhenInputInvalid() {
        assertNull(RecEngineEvaluator.fitLeastSquares(null, null));
        assertNull(RecEngineEvaluator.fitLeastSquares(List.of(), new double[0]));
        assertNull(RecEngineEvaluator.fitLeastSquares(List.of(new double[]{1.0}), new double[]{1.0, 0.0}));
    }

    @Test
    void cvMetricsShouldTrackActualsForWellSpecifiedModel() {
        List<double[]> xs = new ArrayList<>();
        double[] rewards = new double[50];
        for (int i = 0; i < 50; i++) {
            double x = i * 2.0;
            xs.add(new double[]{x});
            rewards[i] = 0.5 * x / 100.0;
        }

        RecEngineEvaluator.CvMetrics cv = RecEngineEvaluator.cvMetrics(xs, rewards);

        assertTrue(cv.mse() < 100.0, "LOO MSE should be small for a well-specified model, got " + cv.mse());
    }

    @Test
    void cvMetricsShouldNotClaimGeneralizationWhenFitMemorized() {
        // 3 samples over 13 signals: the full fit memorizes the rows it was trained on, so its
        // in-sample MSE is small — but a leave-one-out refit on 2 samples cannot generalize.
        int k = SignalWeights.SIGNAL_NAMES.size();
        List<double[]> xs = List.of(oneHot(0, k, 100), oneHot(1, k, 100), new double[k]);
        double[] rewards = new double[]{1.0, 1.0, 0.0};

        double[] w = RecEngineEvaluator.fitLeastSquares(xs, rewards);
        List<Double> preds = new ArrayList<>();
        for (double[] x : xs) {
            preds.add((double) RecEngineEvaluator.predict(w, x));
        }
        List<Double> actual = List.of(1.0, 1.0, 0.0);
        double inSample = RecEngineEvaluator.mse(preds, actual);
        RecEngineEvaluator.CvMetrics cv = RecEngineEvaluator.cvMetrics(xs, rewards);

        assertTrue(cv.mse() > inSample,
                "LOO must report worse metrics than the in-sample fit, got " + cv.mse() + " vs " + inSample);
    }

    @Test
    void cvMetricsShouldBeNaNWhenTooFewSamples() {
        RecEngineEvaluator.CvMetrics cv = RecEngineEvaluator.cvMetrics(List.of(new double[]{1.0}), new double[]{0.5});

        assertTrue(Double.isNaN(cv.mse()));
        assertTrue(Double.isNaN(cv.logLoss()));
        assertTrue(Double.isNaN(cv.auc()));
    }

    private static double[] oneHot(int index, int size, double value) {
        double[] signals = new double[size];
        signals[index] = value;
        return signals;
    }
}
