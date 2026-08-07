package com.forge.analytics;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.entity.DailyMetric;
import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.analytics.service.AnalyticsService;
import com.forge.analytics.service.MetricSnapshotService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsStreakTest {

    @Mock private UserRepository userRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private RevisionRepository revisionRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;
    @Mock private DailyMetricRepository dailyMetricRepository;
    @Mock private ProblemAttemptRepository problemAttemptRepository;
    @Mock private MetricSnapshotService metricSnapshotService;

    private AnalyticsService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(userRepository, topicRepository, revisionRepository,
                journalRepository, snapshotRepository, dailyMetricRepository,
                problemAttemptRepository, metricSnapshotService);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("streak");
        user.setTimezone("UTC");
    }

    @Test
    void streakComesFromASingleBoundedDateQueryNotPerDay() {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zone);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(metricSnapshotService.snapshotForUser(userId)).thenReturn(new DailyMetric());
        when(journalRepository.findEntryDatesByUserIdBetween(eq(userId), any(), any()))
                .thenReturn(List.of(today, today.minusDays(1), today.minusDays(2)));
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(new PageImpl<>(List.of()));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findStrongTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.countByUserId(userId)).thenReturn(0L);
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(eq(userId), any(), any()))
                .thenReturn(List.of());
        when(problemAttemptRepository.countByUserIdAndOutcome(userId, "SOLVED")).thenReturn(0L);
        when(problemAttemptRepository.countByUserIdAndOutcome(userId, "PARTIAL")).thenReturn(0L);
        when(problemAttemptRepository.countByUserId(userId)).thenReturn(0L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            AnalyticsResponse response = service.getAnalytics();

            assertEquals(3, response.getCurrentStreak());
            verify(journalRepository).findEntryDatesByUserIdBetween(eq(userId),
                    eq(today.minusDays(365)), eq(today));
            verify(journalRepository, never()).findByUserIdAndEntryDate(any(), any());
        }
    }

    @Test
    void streakStopsAtFirstGap() {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zone);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(metricSnapshotService.snapshotForUser(userId)).thenReturn(new DailyMetric());
        when(journalRepository.findEntryDatesByUserIdBetween(eq(userId), any(), any()))
                .thenReturn(List.of(today, today.minusDays(2)));
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(new PageImpl<>(List.of()));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findStrongTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.countByUserId(userId)).thenReturn(0L);
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(eq(userId), any(), any()))
                .thenReturn(List.of());
        when(problemAttemptRepository.countByUserIdAndOutcome(userId, "SOLVED")).thenReturn(0L);
        when(problemAttemptRepository.countByUserIdAndOutcome(userId, "PARTIAL")).thenReturn(0L);
        when(problemAttemptRepository.countByUserId(userId)).thenReturn(0L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            AnalyticsResponse response = service.getAnalytics();

            assertEquals(1, response.getCurrentStreak());
            verify(journalRepository, times(1)).findEntryDatesByUserIdBetween(any(), any(), any());
        }
    }
}
