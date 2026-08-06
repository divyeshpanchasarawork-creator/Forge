package com.forge.common.util;

import java.util.List;

/**
 * Weights for the 13 recommendation-score signals, in the exact order the breakdown
 * emits them ({@link #SIGNAL_NAMES}). Calibration adjusts these weights over time;
 * {@link #DEFAULT} is the initial hand-tuned vector.
 */
public record SignalWeights(double weakTag, double masteryGap, double difficultyFit, double learningGain,
                            double revisionUrgency, double confidenceDecay, double readiness,
                            double timeSincePractice, double coverageBalance, double goalAlignment,
                            double notSuggested, double diversity, double ucb) {

    public static final List<String> SIGNAL_NAMES = List.of(
            "Weak tag",
            "Mastery gap",
            "Difficulty fit",
            "Learning gain",
            "Revision urgency",
            "Confidence decay",
            "Readiness",
            "Time since practice",
            "Coverage balance",
            "Goal alignment",
            "Not suggested",
            "Diversity",
            "UCB exploration");

    public static final SignalWeights DEFAULT = new SignalWeights(
            0.15, 0.12, 0.10, 0.10, 0.10, 0.08, 0.08, 0.08, 0.07, 0.06, 0.04, 0.02, 0.10);

    public double[] toArray() {
        return new double[]{weakTag, masteryGap, difficultyFit, learningGain, revisionUrgency,
                confidenceDecay, readiness, timeSincePractice, coverageBalance, goalAlignment,
                notSuggested, diversity, ucb};
    }

    public static SignalWeights from(double[] w) {
        return new SignalWeights(w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7], w[8], w[9], w[10], w[11], w[12]);
    }
}
