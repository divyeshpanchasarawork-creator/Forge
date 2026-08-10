package com.forge.analytics.service;

import com.forge.analytics.entity.DailyMetric;
import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.journal.repository.JournalRepository;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricSnapshotServiceTest {

    @Mock private DailyMetricRepository dailyMetricRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ProblemAttemptRepository problemAttemptRepository;
    @Mock private RevisionRepository revisionRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();

    private MetricSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new MetricSnapshotService(
                dailyMetricRepository,
                topicRepository,
                problemAttemptRepository,
                revisionRepository,
                journalRepository,
                new ForgettingCurveService(topicRepository, userRepository),
                new SkillRatingService(),
                userRepository);
    }

    @Test
    void partialAttemptsCountAsHalfInSolvedDelta() {
        User user = new User();
        user.setId(userId);
        user.setTimezone("UTC");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(problemAttemptRepository.countByUserIdAndOutcomeAndAttemptedAtBetween(
                eq(userId), eq("SOLVED"), any(), any())).thenReturn(2L);
        when(problemAttemptRepository.countByUserIdAndOutcomeAndAttemptedAtBetween(
                eq(userId), eq("PARTIAL"), any(), any())).thenReturn(1L);
        when(dailyMetricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.snapshotForUser(userId);

        ArgumentCaptor<DailyMetric> captor = ArgumentCaptor.forClass(DailyMetric.class);
        verify(dailyMetricRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getSolvedDelta());
    }
}
