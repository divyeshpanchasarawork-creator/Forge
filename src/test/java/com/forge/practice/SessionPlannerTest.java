package com.forge.practice;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.intelligence.service.ColdStartService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.practice.dto.PracticeProblemResponse;
import com.forge.practice.service.SessionPlanner;
import com.forge.recommendation.service.CandidatePoolService;
import com.forge.topic.entity.Topic;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SessionPlannerTest {

    private final SkillRatingService skillRatingService = mock(SkillRatingService.class);
    private final SessionPlanner planner = new SessionPlanner(skillRatingService);
    private final UUID userId = UUID.randomUUID();

    private CandidatePoolService.Candidate candidate(String title, String slug, String difficulty, int score) {
        return candidate(title, slug, difficulty, score, "arrays");
    }

    private CandidatePoolService.Candidate candidate(String title, String slug, String difficulty, int score,
                                                     String tagSlug) {
        return new CandidatePoolService.Candidate(
                new ProblemLoader.ProblemEntry(title, slug, difficulty), tagSlug, score,
                new ProblemScorer.ScoreBreakdown(score, List.of()));
    }

    @Test
    void buildShouldPickHighestScoreFirstWhenNoSlotsPreferOtherSegments() {
        List<CandidatePoolService.Candidate> pool = List.of(
                candidate("B", "b", "Medium", 90),
                candidate("A", "a", "Easy", 70));

        List<PracticeProblemResponse> queue = planner.build(userId, pool, Map.of(), List.of(),
                ColdStartService.Profile.BEGINNER, 10);

        assertEquals(2, queue.size());
        assertEquals("b", queue.get(0).getTitleSlug());
        assertEquals(SessionPlanner.SEGMENT_CHALLENGE, queue.get(0).getSegment());
        assertEquals("a", queue.get(1).getTitleSlug());
        assertEquals(SessionPlanner.SEGMENT_WARMUP, queue.get(1).getSegment());
    }

    @Test
    void buildShouldPreferRevisionForDueTopics() {
        Topic revision = new Topic();
        revision.setTitle("Binary Search");
        revision.setEstimatedRetention(40.0);

        List<CandidatePoolService.Candidate> pool = List.of(
                candidate("Two Sum", "two-sum", "Easy", 95),
                candidate("Search Rotated", "search-rotated", "Medium", 90, "binary-search"));

        List<PracticeProblemResponse> queue = planner.build(userId, pool, Map.of(), List.of(revision),
                ColdStartService.Profile.BEGINNER, 10);

        assertEquals(2, queue.size());
        PracticeProblemResponse revisionPick = queue.stream()
                .filter(q -> q.getTitleSlug().equals("search-rotated"))
                .findFirst().orElseThrow();
        assertEquals(SessionPlanner.SEGMENT_REVISION, revisionPick.getSegment());
    }

    @Test
    void buildShouldLimitBeginnerWarmupToTwo() {
        List<CandidatePoolService.Candidate> pool = List.of(
                candidate("A", "a", "Easy", 80),
                candidate("B", "b", "Easy", 70),
                candidate("C", "c", "Easy", 60),
                candidate("D", "d", "Medium", 50));

        List<PracticeProblemResponse> queue = planner.build(userId, pool, Map.of(), List.of(),
                ColdStartService.Profile.BEGINNER, 10);

        long warmups = queue.stream().filter(q -> SessionPlanner.SEGMENT_WARMUP.equals(q.getSegment())).count();
        assertEquals(2, warmups);
        assertEquals(4, queue.size());
    }

    @Test
    void buildShouldHandleImmutablePool() {
        List<CandidatePoolService.Candidate> pool = List.of(
                candidate("B", "b", "Medium", 90),
                candidate("A", "a", "Easy", 70));

        List<PracticeProblemResponse> queue = planner.build(userId, pool, Map.of(), List.of(),
                ColdStartService.Profile.BEGINNER, 10);

        assertEquals(2, queue.size());
    }
}
