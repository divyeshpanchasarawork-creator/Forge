package com.forge.common.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SignalWeightsTest {

    @Test
    void defaultWeightsShouldCoverAllThirteenSignals() {
        assertEquals(13, SignalWeights.SIGNAL_NAMES.size());
        assertEquals(13, SignalWeights.DEFAULT.toArray().length);
    }

    @Test
    void signalNamesShouldMatchBreakdownOrder() {
        assertEquals("Weak tag", SignalWeights.SIGNAL_NAMES.get(0));
        assertEquals("UCB exploration", SignalWeights.SIGNAL_NAMES.get(12));
    }

    @Test
    void defaultWeightsShouldBePositiveAndBounded() {
        for (double w : SignalWeights.DEFAULT.toArray()) {
            assertTrue(w > 0 && w <= 0.5, "weight out of range: " + w);
        }
    }

    @Test
    void toArrayFromShouldRoundTrip() {
        double[] array = SignalWeights.DEFAULT.toArray();
        SignalWeights roundTrip = SignalWeights.from(array);
        assertArrayEquals(array, roundTrip.toArray());
    }

    @Test
    void fromShouldMapIndicesInSignalNameOrder() {
        double[] array = new double[13];
        Arrays.fill(array, 0.1);
        array[12] = 0.5;
        SignalWeights weights = SignalWeights.from(array);
        assertEquals(0.5, weights.ucb());
        assertEquals(0.1, weights.weakTag());
    }
}
