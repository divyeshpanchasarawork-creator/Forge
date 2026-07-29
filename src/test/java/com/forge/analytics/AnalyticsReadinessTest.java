package com.forge.analytics;

import com.forge.analytics.service.AnalyticsService;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsReadinessTest {

    @Mock private UserRepository userRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private RevisionRepository revisionRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;

    private AnalyticsService analyticsService;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                userRepository, topicRepository, revisionRepository,
                journalRepository, snapshotRepository
        );
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setTargetLevel(7);
    }

    @Test
    void analyticsShouldIncludeTargetLevelAndReadiness() {
        LeetCodeSnapshot snapshot = new LeetCodeSnapshot();
        snapshot.setTotalSolved(200);
        snapshot.setEasySolved(50);
        snapshot.setMediumSolved(100);
        snapshot.setHardSolved(50);
        snapshot.setStreak(15);
        snapshot.setTotalActiveDays(60);
        snapshot.setContestAttendedCount(5);
        snapshot.setRanking(50000);

        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle("DP");
        topic.setConfidence(7);
        topic.setMastery(70);
        topic.setCategory("Algorithms");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.of(snapshot));
        when(topicRepository.findByUserId(any(), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(topic))
        );
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findStrongTopicsByUserId(userId)).thenReturn(List.of(topic));
        when(topicRepository.countByUserId(userId)).thenReturn(1L);
        when(revisionRepository.countByUserIdAndCompleted(eq(userId), eq(true))).thenReturn(10L);
        when(revisionRepository.countByUserIdAndCompleted(eq(userId), eq(false))).thenReturn(5L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            var response = analyticsService.getAnalytics();

            assertNotNull(response);
            assertEquals(7, response.getTargetLevel());
            assertNotNull(response.getReadinessScore());
            assertTrue(response.getReadinessScore() >= 0 && response.getReadinessScore() <= 100,
                    "Readiness score should be between 0 and 100");
        }
    }

    @Test
    void analyticsDefaultTargetWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(topicRepository.findByUserId(any(), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of())
        );
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findStrongTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.countByUserId(userId)).thenReturn(0L);
        when(revisionRepository.countByUserIdAndCompleted(eq(userId), eq(true))).thenReturn(0L);
        when(revisionRepository.countByUserIdAndCompleted(eq(userId), eq(false))).thenReturn(0L);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            var response = analyticsService.getAnalytics();

            assertNotNull(response);
            assertEquals(5, response.getTargetLevel(), "Should default to level 5 when user not found");
        }
    }
}
