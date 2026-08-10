package com.forge.calibration.dto;

/**
 * Outcome of a single calibration run. {@code status} is one of
 * {@code SKIPPED} or {@code APPLIED}; the human-readable {@code message} is
 * meant to be surfaced directly to the user so a run is never a silent no-op.
 */
public record CalibrationResult(
        String status,
        String message,
        int sampleCount,
        int minSamples,
        Double before,
        Double after,
        boolean applied) {

    public static CalibrationResult skipped(int sampleCount, int minSamples, String message) {
        return new CalibrationResult("SKIPPED", message, sampleCount, minSamples, null, null, false);
    }

    public static CalibrationResult skipped(int sampleCount, int minSamples,
                                            double before, double after, String message) {
        return new CalibrationResult("SKIPPED", message, sampleCount, minSamples, before, after, false);
    }

    public static CalibrationResult applied(int sampleCount, int minSamples,
                                            double before, double after, String message) {
        return new CalibrationResult("APPLIED", message, sampleCount, minSamples, before, after, true);
    }
}
