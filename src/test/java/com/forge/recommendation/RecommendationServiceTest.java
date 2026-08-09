package com.forge.recommendation;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.BadRequestException;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.RewardModel;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.SignalWeights;
import com.forge.recommendation.dto.GenerateResponse;
import com.forge.recommendation.mapper.RecommendationMapper;
import com.forge.recommendation.repository.RecommendationRepository;
import com.forge.recommendation.service.RecommendationEngine;
import com.forge.recommendation.service.RecommendationService;
import com.forge.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private RecommendationRepository recommendationRepository;
    @Mock private RecommendationEngine recommendationEngine;
    @Mock private RecommendationMapper recommendationMapper;
    @Mock private UserRepository userRepository;
    @Mock private ProblemScorer problemScorer;
    @Mock private ProblemLoader problemLoader;

    private RecommendationService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(recommendationRepository, recommendationEngine,
                recommendationMapper, userRepository, problemScorer, problemLoader);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setDailyGenerationsUsed(0);
        UserPrincipal principal = new UserPrincipal(userId, "testuser", "password", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateShouldReserveSlotBeforeGenerating() {
        user.setDailyGenerationsUsed(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.reserveDailyGeneration(eq(userId), any(LocalDate.class), eq(4))).thenReturn(1);
        when(recommendationEngine.generateForUser(userId, true)).thenReturn(List.of());
        when(problemScorer.context(userId)).thenReturn(new ProblemScorer.ScoringContext(
                List.of(), List.of(), List.of(), List.of(), 5, RewardModel.stats(List.of()), SignalWeights.DEFAULT,
                java.time.ZoneId.of("UTC")));

        GenerateResponse resp = service.generateRecommendations();

        assertEquals(3, resp.getRemainingGenerations());
        assertEquals(4, resp.getDailyLimit());
        verify(userRepository).reserveDailyGeneration(eq(userId), any(LocalDate.class), eq(4));
        verify(recommendationEngine).generateForUser(userId, true);
        verify(userRepository, never()).releaseDailyGeneration(any(), any());
    }

    @Test
    void generateShouldThrowWhenLimitReachedAndNeverCallEngine() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.reserveDailyGeneration(eq(userId), any(LocalDate.class), eq(4))).thenReturn(0);

        assertThrows(BadRequestException.class, () -> service.generateRecommendations());

        verify(recommendationEngine, never()).generateForUser(any(), anyBoolean());
        verify(userRepository, never()).releaseDailyGeneration(any(), any());
    }

    @Test
    void generateShouldReleaseReservedSlotWhenGenerationFails() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.reserveDailyGeneration(eq(userId), any(LocalDate.class), eq(4))).thenReturn(1);
        when(recommendationEngine.generateForUser(userId, true)).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> service.generateRecommendations());

        verify(userRepository).releaseDailyGeneration(eq(userId), any(LocalDate.class));
    }
}
