package com.forge.scheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisSchedulerTest {

    @Test
    void shouldFireAtExactPreferredTime() {
        assertTrue(AnalysisScheduler.isInWindow(LocalTime.of(6, 0), LocalTime.of(6, 0)));
    }

    @Test
    void shouldFireAtUpperWindowBoundary() {
        assertTrue(AnalysisScheduler.isInWindow(LocalTime.of(6, 15), LocalTime.of(6, 0)));
    }

    @Test
    void shouldNotFireAtLowerWindowBoundary() {
        // Lower bound is exclusive: 05:45 fires at the 05:30 run (upper bound inclusive),
        // so it must NOT fire again at the 06:00 run.
        assertFalse(AnalysisScheduler.isInWindow(LocalTime.of(5, 45), LocalTime.of(6, 0)));
        assertTrue(AnalysisScheduler.isInWindow(LocalTime.of(5, 45), LocalTime.of(5, 30)));
    }

    @Test
    void shouldFireOnlyOnceWhenPreferredMatchesSpanningRun() {
        // 06:00 fires at the 06:00 run (window 05:45..06:15 inclusive of upper bound)...
        assertTrue(AnalysisScheduler.isInWindow(LocalTime.of(6, 0), LocalTime.of(6, 0)));
        // ...but NOT at the 06:30 run (window 06:15..06:45), where the lower bound is exclusive.
        assertFalse(AnalysisScheduler.isInWindow(LocalTime.of(6, 0), LocalTime.of(6, 30)));
    }

    @Test
    void shouldNotFireAtMidnightWrapOverlap() {
        // 23:45 preferred, run at 00:00: window wraps to 23:45..00:15 (exclusive lower) -> no fire.
        assertFalse(AnalysisScheduler.isInWindow(LocalTime.of(23, 45), LocalTime.of(0, 0)));
    }

    @Test
    void shouldFireOutsideInclusiveWindowStart() {
        assertFalse(AnalysisScheduler.isInWindow(LocalTime.of(6, 14), LocalTime.of(6, 30)));
        assertFalse(AnalysisScheduler.isInWindow(LocalTime.of(6, 16), LocalTime.of(6, 0)));
    }
}
