package com.forge.practice;

import com.forge.common.util.ProblemScorer;
import com.forge.common.util.RewardModel;
import com.forge.common.util.SecurityUtils;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.MasteryService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.knowledge.service.KnowledgeGraphService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.dto.PracticeQueueResponse;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.practice.service.PracticeService;
import com.forge.practice.service.SessionPlanner;
import com.forge.recommendation.service.CandidatePoolService;
import com.forge.recommendation.service.RecommendationService;
import com.forge.security.UserPrincipal;
import com.forge.auth.repository.UserRepository;
import com.forge.topic.repository.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    @Mock private ProblemScorer problemScorer;
    @Mock private ProblemAttemptRepository problemAttemptRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;
    @Mock private ColdStartService coldStartService;
    @Mock private SessionPlanner sessionPlanner;
    @Mock private MasteryService masteryService;
    @Mock private SkillRatingService skillRatingService;
    @Mock private ForgettingCurveService forgettingCurveService;
    @Mock private KnowledgeGraphService knowledgeGraphService;
    @Mock private RecommendationService recommendationService;
    @Mock private CandidatePoolService candidatePoolService;

    private PracticeService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PracticeService(problemScorer, problemAttemptRepository, topicRepository,
                userRepository, coldStartService, sessionPlanner, masteryService, skillRatingService,
                forgettingCurveService, knowledgeGraphService, recommendationService, candidatePoolService);
        userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPracticeQueueShouldBuildSingleContextAndNotRequery() {
        when(coldStartService.needsSeed(userId)).thenReturn(false);
        when(coldStartService.classify(userId)).thenReturn(ColdStartService.Profile.BEGINNER);
        when(coldStartService.planMessage(eq(userId), any())).thenReturn("plan");
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(List.of(), List.of(), List.of(), List.of(), 5, RewardModel.stats(List.of())));
        when(candidatePoolService.rankForUser(any(), anyInt())).thenReturn(List.of());
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(new PageImpl<>(List.of()));
        when(sessionPlanner.build(any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());

        PracticeQueueResponse response = service.getPracticeQueue();

        assertNotNull(response);
        assertEquals("beginner", response.getProfile());
        verify(problemScorer).context(userId);
        verify(problemAttemptRepository, never()).findByUserIdAll(any());
        verify(problemAttemptRepository, never()).findByUserIdOrderByAttemptedAtDesc(any(), any());
    }

    @Test
    void getPracticeQueueShouldDeriveAttemptCountsFromContext() {
        com.forge.practice.entity.ProblemAttempt attempt = new com.forge.practice.entity.ProblemAttempt();
        attempt.setProblemSlug("two-sum");
        attempt.setOutcome("SOLVED");
        attempt.setAttemptedAt(java.time.LocalDateTime.now());

        when(coldStartService.needsSeed(userId)).thenReturn(false);
        when(coldStartService.classify(userId)).thenReturn(ColdStartService.Profile.BEGINNER);
        when(coldStartService.planMessage(eq(userId), any())).thenReturn("plan");
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(List.of(), List.of(), List.of(attempt), List.of(), 5, RewardModel.stats(List.of(attempt))));
        when(candidatePoolService.rankForUser(any(), anyInt())).thenReturn(List.of());
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(new PageImpl<>(List.of()));
        when(sessionPlanner.build(any(), any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            Map<String, SessionPlanner.AttemptCounts> counts = inv.getArgument(2);
            assertNotNull(counts);
            assertEquals(1, counts.get("two-sum").attempts());
            assertEquals(1, counts.get("two-sum").solved());
            return List.<PracticeProblemResponse>of();
        });

        service.getPracticeQueue();

        verify(problemAttemptRepository, never()).findByUserIdAll(any());
    }
}
