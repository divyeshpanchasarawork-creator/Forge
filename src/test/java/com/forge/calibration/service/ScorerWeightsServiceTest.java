package com.forge.calibration.service;

import com.forge.calibration.entity.ScorerWeights;
import com.forge.calibration.repository.ScorerWeightsRepository;
import com.forge.common.util.SignalWeights;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScorerWeightsServiceTest {

    @Mock private ScorerWeightsRepository repository;

    private ScorerWeightsService service;

    @BeforeEach
    void setUp() {
        service = new ScorerWeightsService(repository);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    private ScorerWeights storedRow(String json) {
        ScorerWeights row = new ScorerWeights();
        row.setWeightsJson(json);
        row.setVersion(3);
        return row;
    }

    @Test
    void currentWeightsFallsBackToDefaultsWithoutStoredRow() {
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        assertEquals(SignalWeights.DEFAULT, service.currentWeights());
    }

    @Test
    void currentWeightsParsesAndCachesStoredRow() {
        double[] stored = {0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.05, 0.05, 0.04, 0.03, 0.03};
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(storedRow("[0.2,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.05,0.05,0.04,0.03,0.03]")));

        SignalWeights first = service.currentWeights();
        SignalWeights second = service.currentWeights();

        assertEquals(SignalWeights.from(stored), first);
        assertEquals(SignalWeights.from(stored), second);
        verify(repository, times(1)).findFirstByOrderByCreatedAtDesc();
    }

    @Test
    void currentWeightsFallsBackOnUnparseableRow() {
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(storedRow("not-json")));

        assertEquals(SignalWeights.DEFAULT, service.currentWeights());
    }

    @Test
    void applyWeightsPersistsRowAndBumpsVersion() {
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.applyWeights(SignalWeights.DEFAULT, 25, 1.5, 0.8);

        verify(repository).save(argThat(row -> {
            assertNotNull(row.getWeightsJson());
            assertEquals(2, row.getVersion());
            assertEquals(25, row.getSampleCount());
            assertEquals(1.5, row.getMetricBefore());
            assertEquals(0.8, row.getMetricAfter());
            assertNotNull(row.getLastCalibratedAt());
            return true;
        }));
    }

    @Test
    void applyWeightsWithoutTransactionPublishesCacheImmediately() {
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SignalWeights candidate = SignalWeights.from(new double[]{0.9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.1});

        service.applyWeights(candidate, 10, 2.0, 1.0);

        assertEquals(candidate, service.currentWeights());
    }

    @Test
    void applyWeightsInTransactionPublishesCacheOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SignalWeights candidate = SignalWeights.from(new double[]{0.9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.1});

        service.applyWeights(candidate, 10, 2.0, 1.0);
        assertEquals(SignalWeights.DEFAULT, service.currentWeights());

        // In Spring 6, triggerAfterCommit was removed; invoke synchronizations manually
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertEquals(candidate, service.currentWeights());
    }

    @Test
    void recordMetricsNeverCreatesRowWhenNoneExists() {
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        service.recordMetrics(12, 2.5, 2.5);

        verify(repository, never()).save(any());
    }

    @Test
    void recordMetricsUpdatesExistingRow() {
        ScorerWeights row = storedRow("[]");
        when(repository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(row));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordMetrics(12, 2.5, 2.6);

        verify(repository).save(argThat(r -> {
            assertEquals(12, r.getSampleCount());
            assertEquals(2.5, r.getMetricBefore());
            assertEquals(2.6, r.getMetricAfter());
            return true;
        }));
    }
}
