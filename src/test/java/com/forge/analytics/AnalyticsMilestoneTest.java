package com.forge.analytics;

import com.forge.analytics.dto.LearningCurveResponse;
import com.forge.analytics.entity.DailyMetric;
import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.analytics.service.AnalyticsService;
import com.forge.analytics.service.MetricSnapshotService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.security.UserPrincipal;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsMilestoneTest {

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

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(userRepository, topicRepository, revisionRepository,
                journalRepository, snapshotRepository, dailyMetricRepository,
                problemAttemptRepository, metricSnapshotService);
        userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setTimezone("UTC");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(metricSnapshotService.snapshotForUser(userId)).thenReturn(new DailyMetric());
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skillMilestonesAreReportedOnlyOnceAcrossTheCurve() {
        LocalDate today = LocalDate.now();
        List<DailyMetric> metrics = new ArrayList<>();
        double[] skills = {1000, 1050, 1120, 1180, 1250, 1350, 1420};
        for (int i = 0; i < skills.length; i++) {
            DailyMetric m = new DailyMetric();
            m.setMetricDate(today.minusDays(6L - i));
            m.setSkillRating(skills[i]);
            metrics.add(m);
        }
        when(dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(eq(userId), any(), any()))
                .thenReturn(metrics);
        when(problemAttemptRepository.findFirstByUserIdAndOutcomeAndDifficultyOrderByAttemptedAtAsc(
                eq(userId), eq("SOLVED"), eq("HARD"))).thenReturn(Optional.empty());

        LearningCurveResponse response = service.getLearningCurve(90);

        long skillMilestones = response.getMilestones().stream()
                .filter(m -> "SKILL".equals(m.getType()))
                .count();
        assertEquals(2, skillMilestones, "Each skill threshold should be reported exactly once");
        assertEquals(1, response.getMilestones().stream()
                        .filter(m -> m.getType().equals("SKILL") && m.getLabel().contains("1100")).count(),
                "1100 crossing must appear once even while skill stays in [1100, 1200)");
        assertEquals(1, response.getMilestones().stream()
                        .filter(m -> m.getType().equals("SKILL") && m.getLabel().contains("1400")).count(),
                "1400 crossing must appear once even while skill stays in [1400, 1500)");
    }
}
