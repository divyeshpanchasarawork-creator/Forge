package com.forge.calibration.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
