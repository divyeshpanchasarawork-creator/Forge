package com.forge.recommendation;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.repository.RecommendationRepository;
import com.forge.recommendation.service.RecommendationEngine;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationEngineTest {

    @Mock private TopicRepository topicRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeetCodeSnapshotRepository snapshotRepository;
    @Mock private LeetCodeTagStatRepository tagStatRepository;
    @Mock private ProblemSuggestionRepository problemSuggestionRepository;
    @Mock private ProblemLoader problemLoader;
    @Mock private ProblemScorer problemScorer;

    private RecommendationEngine engine;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine(topicRepository, recommendationRepository, userRepository, snapshotRepository, tagStatRepository, problemSuggestionRepository, problemLoader, problemScorer);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setTargetLevel(5);
    }

    @Test
    void shouldGenerateLowConfidenceRecommendations() {
        Topic weakTopic = new Topic();
        weakTopic.setId(UUID.randomUUID());
        weakTopic.setTitle("Dynamic Programming");
        weakTopic.setConfidence(2);
        weakTopic.setMastery(20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of(weakTopic));
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());

        List<Recommendation> recs = engine.generateForUser(userId, false);

        assertFalse(recs.isEmpty());
        assertTrue(recs.stream().anyMatch(r -> r.getTitle().contains("Dynamic Programming")));
        assertEquals(1, recs.stream().filter(r -> r.getAction().equals("REVIEW")).count());
    }

    @Test
    void shouldReturnNoRecommendationsWhenAllConfident() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());

        List<Recommendation> recs = engine.generateForUser(userId, false);
        assertTrue(recs.isEmpty());
    }

    @Test
    void shouldGenerateLeetCodeBasedRecommendations() {
        LeetCodeSnapshot snapshot = new LeetCodeSnapshot();
        snapshot.setTotalSolved(25);
        snapshot.setEasySolved(20);
        snapshot.setMediumSolved(4);
        snapshot.setHardSolved(1);
        snapshot.setStreak(0);
        snapshot.setTotalActiveDays(10);
        snapshot.setContestAttendedCount(0);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.of(snapshot));
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of());

        List<Recommendation> recs = engine.generateForUser(userId, false);

        assertFalse(recs.isEmpty());
        long lcRecCount = recs.stream()
                .filter(r -> List.of("LEVEL_UP", "START_STREAK", "TRY_CONTEST", "MILESTONE", "TRY_HARD", "PRACTICE_TAG")
                        .contains(r.getAction()))
                .count();
        assertTrue(lcRecCount > 0, "Expected LeetCode-based recommendations");
    }

    @Test
    void shouldPersistWhenFlagSet() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());
        doNothing().when(recommendationRepository).deleteByUserIdAndDismissed(any(), anyBoolean());
        when(recommendationRepository.saveAll(any())).thenReturn(List.of());
        when(recommendationRepository.findByUserIdAndDismissedOrderByPriorityAscCreatedAtDesc(userId, false))
                .thenReturn(List.of());

        List<Recommendation> recs = engine.generateForUser(userId, true);

        verify(recommendationRepository).deleteByUserIdAndDismissed(userId, false);
        verify(recommendationRepository).saveAll(any());
    }

    @Test
    void shouldNotPersistWhenFlagFalse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());

        engine.generateForUser(userId, false);

        verify(recommendationRepository, never()).deleteByUserIdAndDismissed(any(), anyBoolean());
        verify(recommendationRepository, never()).saveAll(any());
    }

    @Test
    void higherTargetLevelShouldGenerateMoreAggressiveRecs() {
        user.setTargetLevel(8);

        Topic midTopic = new Topic();
        midTopic.setId(UUID.randomUUID());
        midTopic.setTitle("Binary Trees");
        midTopic.setConfidence(5);
        midTopic.setMastery(50);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findByUserId(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(midTopic)));
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());

        List<Recommendation> recs = engine.generateForUser(userId, false);

        assertTrue(recs.stream().anyMatch(r -> r.getTitle().contains("Deepen")),
                "High target should include deepen recommendations for mid-confidence topics");
    }

    @Test
    void lowTargetShouldNotDeepenMidConfidenceTopics() {
        user.setTargetLevel(2);

        Topic midTopic = new Topic();
        midTopic.setId(UUID.randomUUID());
        midTopic.setTitle("Binary Trees");
        midTopic.setConfidence(5);
        midTopic.setMastery(50);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.empty());

        List<Recommendation> recs = engine.generateForUser(userId, false);

        assertTrue(recs.stream().noneMatch(r -> r.getTitle().contains("Deepen")),
                "Low target should not generate deepen recommendations");
    }

    @Test
    void shouldCreateDifficultyGapRecsForTargetLevel() {
        user.setTargetLevel(8);

        LeetCodeSnapshot snapshot = new LeetCodeSnapshot();
        snapshot.setTotalSolved(100);
        snapshot.setEasySolved(60);
        snapshot.setMediumSolved(30);
        snapshot.setHardSolved(10);
        snapshot.setStreak(5);
        snapshot.setTotalActiveDays(30);
        snapshot.setContestAttendedCount(1);

        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle("Arrays");
        topic.setConfidence(8);
        topic.setMastery(80);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(topicRepository.findWeakTopicsByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findTopicsNeedingRevisionByUserId(userId)).thenReturn(List.of());
        when(topicRepository.findByUserId(any(), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(topic))
        );
        when(snapshotRepository.findByUserId(userId)).thenReturn(Optional.of(snapshot));
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of());

        List<Recommendation> recs = engine.generateForUser(userId, false);

        boolean hasHardRec = recs.stream().anyMatch(r -> r.getTitle().toLowerCase().contains("hard problem"));
        assertTrue(hasHardRec, "For level 8, should recommend Hard problems when count is low");
    }
}
