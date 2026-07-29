package com.forge.dashboard;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.dashboard.dto.DashboardResponse;
import com.forge.dashboard.service.DashboardService;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.recommendation.service.RecommendationService;
import com.forge.revision.service.RevisionService;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import com.forge.topic.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadinessScoreTest {

    @Mock private UserRepository userRepository;
    @Mock private TopicService topicService;
    @Mock private RevisionService revisionService;
    @Mock private RecommendationService recommendationService;
    @Mock private JournalRepository journalRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;

    private DashboardService dashboardService;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                userRepository, topicService, revisionService, recommendationService,
                journalRepository, topicRepository, snapshotRepository
        );
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setTargetLevel(5);
    }

    @Test
    void readinessScoreShouldBeHighForTargetLevel1WithFewProblems() {
        user.setTargetLevel(1);

        LeetCodeSnapshot snapshot = new LeetCodeSnapshot();
        snapshot.setTotalSolved(45);
        snapshot.setEasySolved(40);
        snapshot.setMediumSolved(5);
        snapshot.setHardSolved(0);
        snapshot.setStreak(10);
        snapshot.setTotalActiveDays(30);
        snapshot.setContestAttendedCount(1);

        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle("Arrays");
        topic.setConfidence(8);
        topic.setMastery(80);
        topic.setStatus("MASTERED");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationService.getActiveRecommendations()).thenReturn(List.of());
        when(topicService.getWeakTopics()).thenReturn(List.of());
        when(topicService.getStrongTopics()).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.of(snapshot));
        when(topicRepository.findByUserId(any(), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(topic))
        );
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            DashboardResponse response = dashboardService.getDashboard();

            assertNotNull(response);
            assertNotNull(response.getTargetProgress());
            assertTrue(response.getTargetProgress().getReadinessScore() >= 50,
                    "Level 1 with 45/50 problems should have readiness >= 50");
        }
    }

    @Test
    void readinessScoreShouldBeLowForHighTargetWithFewProblems() {
        user.setTargetLevel(8);

        LeetCodeSnapshot snapshot = new LeetCodeSnapshot();
        snapshot.setTotalSolved(50);
        snapshot.setEasySolved(30);
        snapshot.setMediumSolved(15);
        snapshot.setHardSolved(5);
        snapshot.setStreak(3);
        snapshot.setTotalActiveDays(20);
        snapshot.setContestAttendedCount(0);

        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle("Arrays");
        topic.setConfidence(6);
        topic.setMastery(60);
        topic.setStatus("IN_PROGRESS");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationService.getActiveRecommendations()).thenReturn(List.of());
        when(topicService.getWeakTopics()).thenReturn(List.of());
        when(topicService.getStrongTopics()).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.of(snapshot));
        when(topicRepository.findByUserId(any(), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(topic))
        );
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            DashboardResponse response = dashboardService.getDashboard();

            assertNotNull(response);
            assertNotNull(response.getTargetProgress());
            assertTrue(response.getTargetProgress().getReadinessScore() < 70,
                    "Level 8 with only 50 problems should have readiness < 70");
        }
    }

    @Test
    void targetProgressShouldContainCorrectDifficultyGap() {
        user.setTargetLevel(5);

        LeetCodeSnapshot snapshot = new LeetCodeSnapshot();
        snapshot.setTotalSolved(100);
        snapshot.setEasySolved(40);
        snapshot.setMediumSolved(40);
        snapshot.setHardSolved(20);
        snapshot.setStreak(5);
        snapshot.setTotalActiveDays(30);
        snapshot.setContestAttendedCount(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationService.getActiveRecommendations()).thenReturn(List.of());
        when(topicService.getWeakTopics()).thenReturn(List.of());
        when(topicService.getStrongTopics()).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.of(snapshot));
        when(topicRepository.findByUserId(any(), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of())
        );
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            DashboardResponse response = dashboardService.getDashboard();

            DashboardResponse.DifficultyGap gap = response.getTargetProgress().getDifficultyGap();
            assertNotNull(gap);
            assertEquals(40, gap.getCurrentEasy());
            assertEquals(40, gap.getCurrentMedium());
            assertEquals(20, gap.getCurrentHard());
            assertTrue(response.getTargetProgress().getTargetTotal() > 0);
        }
    }
}
