package com.forge.recommendation;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.recommendation.service.CandidatePoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatePoolServiceTest {

    @Mock private ProblemLoader problemLoader;
    @Mock private ProblemScorer problemScorer;
    @Mock private LeetCodeTagStatRepository tagStatRepository;

    private CandidatePoolService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new CandidatePoolService(problemLoader, problemScorer, tagStatRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void rankShouldSortAndLimitByScore() {
        ProblemLoader.ProblemEntry low = new ProblemLoader.ProblemEntry("Low", "low", "Medium");
        ProblemLoader.ProblemEntry high = new ProblemLoader.ProblemEntry("High", "high", "Easy");
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(List.of(low, high));
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(null, List.of(), List.of(), List.of(), List.of(), 5));
        when(problemScorer.breakdown(any(), any(), any())).thenAnswer(inv -> {
            ProblemLoader.ProblemEntry p = inv.getArgument(1);
            int total = p.getTitle().equals("High") ? 90 : 40;
            return new ProblemScorer.ScoreBreakdown(total, List.of());
        });

        List<CandidatePoolService.Candidate> ranked = service.rank(userId, List.of("arrays"), null, 1);

        assertEquals(1, ranked.size());
        assertEquals("High", ranked.getFirst().problem().getTitle());
        assertEquals(90, ranked.getFirst().score());
    }

    @Test
    void bestProblemShouldUseWeakTagsWhenNoTagGiven() {
        LeetCodeTagStat weak = new LeetCodeTagStat();
        weak.setTagSlug("dp");
        weak.setProblemsSolved(2);
        LeetCodeTagStat strong = new LeetCodeTagStat();
        strong.setTagSlug("arrays");
        strong.setProblemsSolved(20);
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of(weak, strong));

        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Coin Change", "coin-change", "Medium");
        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of(p));
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(null, List.of(), List.of(), List.of(), List.of(), 5));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(75, List.of()));

        Optional<CandidatePoolService.Candidate> best = service.bestProblem(userId, null, null);

        assertTrue(best.isPresent());
        assertEquals("coin-change", best.get().problem().getTitleSlug());
        verify(problemScorer).breakdown(any(), any(), eq("dp"));
    }

    @Test
    void bestProblemShouldFallBackToAllTagsWhenNoneWeak() {
        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagSlug("arrays");
        tag.setProblemsSolved(8);
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of(tag));

        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy");
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(List.of(p));
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(null, List.of(), List.of(), List.of(), List.of(), 5));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(60, List.of()));

        Optional<CandidatePoolService.Candidate> best = service.bestProblem(userId, null, null);

        assertTrue(best.isPresent());
        assertEquals("two-sum", best.get().problem().getTitleSlug());
    }

    @Test
    void bestProblemForTopicShouldResolveTagViaTagStatsWhenSlugEmpty() {
        when(problemLoader.getProblemsForTag("binary-trees")).thenReturn(List.of());

        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagName("Binary Trees");
        tag.setTagSlug("binary-tree");
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of(tag));

        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Invert Tree", "invert-tree", "Easy");
        when(problemLoader.getProblemsForTag("binary-tree")).thenReturn(List.of(p));
        when(problemScorer.context(userId)).thenReturn(
                new ProblemScorer.ScoringContext(null, List.of(), List.of(), List.of(), List.of(), 5));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(80, List.of()));

        Optional<CandidatePoolService.Candidate> best = service.bestProblemForTopic(userId, "Binary Trees");

        assertTrue(best.isPresent());
        assertEquals("invert-tree", best.get().problem().getTitleSlug());
    }

    @Test
    void bestProblemForTopicShouldReturnEmptyWhenNoMatch() {
        when(problemLoader.getProblemsForTag("binary-trees")).thenReturn(List.of());
        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagName("Arrays");
        tag.setTagSlug("arrays");
        when(tagStatRepository.findByUserId(userId)).thenReturn(List.of(tag));

        Optional<CandidatePoolService.Candidate> best = service.bestProblemForTopic(userId, "Binary Trees");

        assertTrue(best.isEmpty());
    }
}
