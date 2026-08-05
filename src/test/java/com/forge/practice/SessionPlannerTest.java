package com.forge.practice;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.service.SessionPlanner;
import com.forge.recommendation.service.CandidatePoolService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SessionPlannerTest {

    @Test
    void buildShouldHandleImmutablePool() {
        SkillRatingService skillRatingService = mock(SkillRatingService.class);
        SessionPlanner planner = new SessionPlanner(skillRatingService);
        UUID userId = UUID.randomUUID();

        List<CandidatePoolService.Candidate> pool = List.of(
                new CandidatePoolService.Candidate(
                        new ProblemLoader.ProblemEntry("B", "b", "Medium"), "arrays", 90,
                        new ProblemScorer.ScoreBreakdown(90, List.of())),
                new CandidatePoolService.Candidate(
                        new ProblemLoader.ProblemEntry("A", "a", "Easy"), "arrays", 70,
                        new ProblemScorer.ScoreBreakdown(70, List.of())));

        List<PracticeProblemResponse> queue = planner.build(userId, pool, Map.of(), List.of(),
                ColdStartService.Profile.BEGINNER, 10);

        assertEquals(2, queue.size());
        assertEquals("a", queue.get(0).getTitleSlug());
        assertEquals("b", queue.get(1).getTitleSlug());
    }
}
