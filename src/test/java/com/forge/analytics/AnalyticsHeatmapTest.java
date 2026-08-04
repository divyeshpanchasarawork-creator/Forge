package com.forge.analytics;

import com.forge.analytics.dto.ActivityDay;
import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.analytics.service.AnalyticsService;
import com.forge.analytics.service.MetricSnapshotService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.entity.Journal;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsHeatmapTest {

    @Mock private UserRepository userRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private RevisionRepository revisionRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;
    @Mock private DailyMetricRepository dailyMetricRepository;
    @Mock private ProblemAttemptRepository problemAttemptRepository;
    @Mock private MetricSnapshotService metricSnapshotService;

    private AnalyticsService analyticsService;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                userRepository, topicRepository, revisionRepository,
                journalRepository, snapshotRepository, dailyMetricRepository, problemAttemptRepository,
                metricSnapshotService
        );
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("heatmap");
        user.setTimezone("Asia/Kolkata");
    }

    @Test
    void heatmapMergesJournalsAttemptsAndRevisionsForUserZone() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);

        Journal journal = new Journal();
        journal.setEntryDate(today);
        journal.setHoursStudied(1.5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(journalRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(eq(userId), any(), any()))
                .thenReturn(List.of(journal));
        when(problemAttemptRepository.findAttemptedAtInRangeByUserId(eq(userId), any(), any()))
                .thenReturn(List.of(today.atTime(18, 0)));
        when(revisionRepository.findCompletedDatesInRangeByUserId(eq(userId), any(), any()))
                .thenReturn(List.of(today));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            List<ActivityDay> days = analyticsService.getActivityHeatmap(28);

            assertEquals(196, days.size(), "28 weeks of daily cells");

            ActivityDay activeDay = days.stream()
                    .filter(d -> d.date().equals(today))
                    .findFirst().orElseThrow();
            assertTrue(activeDay.active());
            assertEquals(1.5, activeDay.hours());
            assertEquals(1, activeDay.attempts());
            assertEquals(1, activeDay.revisions());

            long activeCount = days.stream().filter(ActivityDay::active).count();
            assertEquals(1, activeCount, "only today should be active");

            LocalDate twoWeeksAgo = today.minusDays(14);
            ActivityDay quietDay = days.stream()
                    .filter(d -> d.date().equals(twoWeeksAgo))
                    .findFirst().orElseThrow();
            assertFalse(quietDay.active());
            assertEquals(0, quietDay.hours());
        }
    }

    @Test
    void heatmapStartsOnSundayAndIsConsecutive() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(journalRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(eq(userId), any(), any()))
                .thenReturn(List.of());
        when(problemAttemptRepository.findAttemptedAtInRangeByUserId(eq(userId), any(), any()))
                .thenReturn(List.of());
        when(revisionRepository.findCompletedDatesInRangeByUserId(eq(userId), any(), any()))
                .thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            List<ActivityDay> days = analyticsService.getActivityHeatmap(28);

            assertEquals(196, days.size());
            assertEquals(java.time.DayOfWeek.SUNDAY, days.getFirst().date().getDayOfWeek(),
                    "grid starts on a Sunday");
            for (int i = 1; i < days.size(); i++) {
                assertEquals(days.get(i - 1).date().plusDays(1), days.get(i).date(),
                        "days must be consecutive");
            }
        }
    }
}
