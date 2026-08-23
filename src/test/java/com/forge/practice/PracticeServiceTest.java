package com.forge.practice;

import com.forge.common.util.ProblemScorer;
import com.forge.common.util.RewardModel;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.SignalWeights;
import com.forge.common.exception.BadRequestException;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.MasteryService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.knowledge.service.KnowledgeGraphService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.dto.PracticeQueueResponse;
import com.forge.practice.dto.ProblemAttemptRequest;
import com.forge.practice.dto.ProblemAttemptResponse;
import com.forge.practice.dto.ProblemAttemptSummary;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.practice.service.PracticeService;
import com.forge.practice.service.SessionPlanner;
import com.forge.recommendation.service.CandidatePoolService;
import com.forge.recommendation.service.RecommendationService;
import com.forge.security.UserPrincipal;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.topic.entity.Topic;
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
import java.util.Optional;
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
    @Mock private com.forge.leetcode.repository.ExternalSolveRepository externalSolveRepository;

    private PracticeService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PracticeService(problemScorer, problemAttemptRepository, topicRepository,
                userRepository, coldStartService, sessionPlanner, masteryService, skillRatingService,
                forgettingCurveService, knowledgeGraphService, recommendationService, candidatePoolService,
                externalSolveRepository);
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
                        RewardModel.stats(List.of()), SignalWeights.DEFAULT, java.util.Map.of(), java.time.ZoneId.of("UTC")));
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
                        RewardModel.stats(List.of(attempt)), SignalWeights.DEFAULT, java.util.Map.of(), java.time.ZoneId.of("UTC")));
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
        assertEquals("Easy", s.difficulty());
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

    @Test
    void submitAttemptNormalizesOutcomeAndDifficultyBeforePersisting() {
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(masteryService.qualityFrom("SOLVED", 0, 420)).thenReturn(5);
        when(problemAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(List.of());

        ProblemAttemptRequest request = new ProblemAttemptRequest();
        request.setProblemTitle("Two Sum");
        request.setProblemSlug("two-sum");
        request.setDifficulty("EASY");
        request.setOutcome("solved");
        request.setHintsUsed(0);
        request.setTimeTakenSeconds(420);

        ProblemAttemptResponse response = service.submitAttempt(request);

        ArgumentCaptor<ProblemAttempt> captor = ArgumentCaptor.forClass(ProblemAttempt.class);
        verify(problemAttemptRepository).save(captor.capture());
        ProblemAttempt saved = captor.getValue();
        assertEquals("SOLVED", saved.getOutcome());
        assertEquals("Easy", saved.getDifficulty());
        assertEquals(5, saved.getQuality());
        assertEquals("two-sum", saved.getProblemSlug());
        assertNull(saved.getSignalsJson());
        assertEquals("SOLVED Two Sum recorded. No matching topic yet — add one to link your progress.",
                response.getFeedback());
        verify(recommendationService).completeRecommendationsForProblem(eq(userId), eq("two-sum"), eq("SOLVED"));
        verify(externalSolveRepository).markLogged(userId, "two-sum");
    }

    @Test
    void submitAttemptUpdatesMatchingTopicAndPropagatesGraphBoost() {
        User user = new User();
        user.setId(userId);
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setTitle("Two Sum");
        topic.setSkillRating(1000.0);
        topic.setAttemptsTotal(0);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(masteryService.qualityFrom("SOLVED", 0, 300)).thenReturn(5);
        when(problemAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(List.of(topic));
        when(skillRatingService.applyResult(1000.0, "Medium", true, 0)).thenReturn(1016.0);
        when(knowledgeGraphService.matchConcept("Two Sum")).thenReturn("two-sum");

        ProblemAttemptRequest request = new ProblemAttemptRequest();
        request.setProblemTitle("Two Sum");
        request.setProblemSlug("two-sum");
        request.setDifficulty("Medium");
        request.setOutcome("SOLVED");
        request.setHintsUsed(0);
        request.setTimeTakenSeconds(300);

        ProblemAttemptResponse response = service.submitAttempt(request);

        verify(masteryService).apply(topic, "SOLVED", 0);
        assertEquals(1016.0, topic.getSkillRating());
        verify(forgettingCurveService).strengthen(topic, 1.0);
        verify(forgettingCurveService).refreshTopicRetention(topic);
        verify(topicRepository).save(topic);
        verify(knowledgeGraphService).propagateBoost(userId, "two-sum", 15);
        assertEquals(List.of("Two Sum"), response.getTopicsUpdated());
    }

    @Test
    void submitAttemptSnapshotsSignalsEvenWithoutTopicTag() {
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(masteryService.qualityFrom("SOLVED", 0, 300)).thenReturn(5);
        when(problemAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByUserId(eq(userId), any())).thenReturn(List.of());
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(List.of(), List.of(), List.of(), List.of(), 5,
                        RewardModel.stats(List.of()), SignalWeights.DEFAULT, java.util.Map.of(), java.time.ZoneId.of("UTC")));
        when(problemScorer.breakdown(any(), any(), eq(null))).thenReturn(new ProblemScorer.ScoreBreakdown(72,
                List.of(new ProblemScorer.ScoreItem("Weak tag", 0.15, 100, 15))));

        ProblemAttemptRequest request = new ProblemAttemptRequest();
        request.setProblemTitle("Two Sum");
        request.setProblemSlug("two-sum");
        request.setDifficulty("Easy");
        request.setOutcome("SOLVED");
        request.setTimeTakenSeconds(300);

        service.submitAttempt(request);

        ArgumentCaptor<ProblemAttempt> captor = ArgumentCaptor.forClass(ProblemAttempt.class);
        verify(problemAttemptRepository).save(captor.capture());
        assertEquals(72, captor.getValue().getPredictedScore(),
                "attempts without a topic tag must still become calibration samples");
        assertNotNull(captor.getValue().getSignalsJson());
    }

    @Test
    void submitAttemptRejectsInvalidOutcome() {
        ProblemAttemptRequest request = new ProblemAttemptRequest();
        request.setProblemTitle("Two Sum");
        request.setProblemSlug("two-sum");
        request.setOutcome("GUESSED");

        assertThrows(BadRequestException.class, () -> service.submitAttempt(request));
        verify(problemAttemptRepository, never()).save(any());
    }

    @Test
    void submitAttemptRejectsBlankProblemSlug() {
        ProblemAttemptRequest request = new ProblemAttemptRequest();
        request.setProblemTitle("Two Sum");
        request.setProblemSlug("  ");
        request.setOutcome("SOLVED");

        assertThrows(BadRequestException.class, () -> service.submitAttempt(request));
        verify(problemAttemptRepository, never()).save(any());
    }
}
