package com.forge.practice;

import com.forge.common.util.ProblemScorer;
import com.forge.common.util.RewardModel;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.SignalWeights;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.MasteryService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.knowledge.service.KnowledgeGraphService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.dto.PracticeQueueResponse;
import com.forge.practice.dto.ProblemAttemptSummary;
import com.forge.practice.entity.ProblemAttempt;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password", "USER");
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
                new ProblemScorer.ScoringContext(List.of(), List.of(), List.of(), List.of(), 5,
                        RewardModel.stats(List.of()), SignalWeights.DEFAULT, java.time.ZoneId.of("UTC")));
        when(candidatePoolService.rankForUser(any(), anyInt())).thenReturn(List.of());
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(List.of());
        when(sessionPlanner.build(any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());

        PracticeQueueResponse response = service.getPracticeQueue();

        assertNotNull(response);
        assertEquals("beginner", response.getProfile());
        verify(problemScorer).context(userId);
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
                new ProblemScorer.ScoringContext(List.of(), List.of(), List.of(attempt), List.of(), 5,
                        RewardModel.stats(List.of(attempt)), SignalWeights.DEFAULT, java.time.ZoneId.of("UTC")));
        when(candidatePoolService.rankForUser(any(), anyInt())).thenReturn(List.of());
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(List.of());
        when(sessionPlanner.build(any(), any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            Map<String, SessionPlanner.AttemptCounts> counts = inv.getArgument(2);
            assertNotNull(counts);
            assertEquals(1, counts.get("two-sum").attempts());
            assertEquals(1, counts.get("two-sum").solved());
            return List.<PracticeProblemResponse>of();
        });

        service.getPracticeQueue();

        verify(problemAttemptRepository, never()).findByUserIdOrderByAttemptedAtDesc(any(), any());
    }

    @Test
    void getAttemptHistoryMapsEntitiesToSummaries() {
        ProblemAttempt attempt = new ProblemAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setProblemTitle("Two Sum");
        attempt.setProblemSlug("two-sum");
        attempt.setDifficulty("EASY");
        attempt.setTopicTagSlug("arrays");
        attempt.setTopicTagName("Arrays");
        attempt.setOutcome("SOLVED");
        attempt.setHintsUsed(0);
        attempt.setTimeTakenSeconds(420);
        attempt.setQuality(5);
        attempt.setAttemptedAt(java.time.LocalDateTime.of(2026, 8, 12, 10, 0));
        when(problemAttemptRepository.findByUserIdOrderByAttemptedAtDesc(eq(userId), any()))
                .thenReturn(List.of(attempt));

        List<ProblemAttemptSummary> result = service.getAttemptHistory(20);

        assertEquals(1, result.size());
        ProblemAttemptSummary s = result.get(0);
        assertEquals(attempt.getId(), s.id());
        assertEquals("Two Sum", s.problemTitle());
        assertEquals("two-sum", s.problemSlug());
        assertEquals("EASY", s.difficulty());
        assertEquals("arrays", s.topicTagSlug());
        assertEquals("Arrays", s.topicTagName());
        assertEquals("SOLVED", s.outcome());
        assertEquals(0, s.hintsUsed());
        assertEquals(420, s.timeTakenSeconds());
        assertEquals(5, s.quality());
        assertEquals(java.time.LocalDateTime.of(2026, 8, 12, 10, 0), s.attemptedAt());
    }

    @Test
    void getAttemptHistoryClampsLimitToFifty() {
        when(problemAttemptRepository.findByUserIdOrderByAttemptedAtDesc(eq(userId), any()))
                .thenReturn(List.of());

        service.getAttemptHistory(999);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(problemAttemptRepository).findByUserIdOrderByAttemptedAtDesc(eq(userId), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
    }
}
