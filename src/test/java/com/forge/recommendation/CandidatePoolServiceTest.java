package com.forge.recommendation;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.RewardModel;
import com.forge.common.util.SignalWeights;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.recommendation.service.CandidatePoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatePoolServiceTest {

    @Mock private ProblemLoader problemLoader;
    @Mock private ProblemScorer problemScorer;

    private CandidatePoolService service;

    @BeforeEach
    void setUp() {
        service = new CandidatePoolService(problemLoader, problemScorer);
    }

    private ProblemScorer.ScoringContext ctx(List<LeetCodeTagStat> stats) {
        return new ProblemScorer.ScoringContext(stats, List.of(), List.of(), List.of(), 5,
                RewardModel.stats(List.of()), SignalWeights.DEFAULT, java.util.Map.of(), java.time.ZoneId.of("UTC"));
    }

    private ProblemScorer.ScoringContext ctx(List<LeetCodeTagStat> stats, List<ProblemSuggestion> suggestions) {
        return new ProblemScorer.ScoringContext(stats, List.of(), List.of(), suggestions, 5,
                RewardModel.stats(List.of()), SignalWeights.DEFAULT, java.util.Map.of(), java.time.ZoneId.of("UTC"));
    }

    @Test
    void rankShouldSortAndLimitByScore() {
        ProblemLoader.ProblemEntry low = new ProblemLoader.ProblemEntry("Low", "low", "Medium");
        ProblemLoader.ProblemEntry high = new ProblemLoader.ProblemEntry("High", "high", "Easy");
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(List.of(low, high));
        when(problemScorer.breakdown(any(), any(), any())).thenAnswer(inv -> {
            ProblemLoader.ProblemEntry p = inv.getArgument(1);
            int total = p.getTitle().equals("High") ? 90 : 40;
            return new ProblemScorer.ScoreBreakdown(total, List.of());
        });

        List<CandidatePoolService.Candidate> ranked = service.rank(ctx(List.of()), List.of("arrays"), null, 1);

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

        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Coin Change", "coin-change", "Medium");
        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of(p));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(75, List.of()));

        Optional<CandidatePoolService.Candidate> best = service.bestProblem(ctx(List.of(weak, strong)), null, null);

        assertTrue(best.isPresent());
        assertEquals("coin-change", best.get().problem().getTitleSlug());
        verify(problemScorer).breakdown(any(), any(), eq("dp"));
    }

    @Test
    void bestProblemShouldFallBackToAllTagsWhenNoneWeak() {
        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagSlug("arrays");
        tag.setProblemsSolved(8);

        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy");
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(List.of(p));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(60, List.of()));

        Optional<CandidatePoolService.Candidate> best = service.bestProblem(ctx(List.of(tag)), null, null);

        assertTrue(best.isPresent());
        assertEquals("two-sum", best.get().problem().getTitleSlug());
    }

    @Test
    void bestProblemForTopicShouldResolveTagViaTagStatsWhenSlugEmpty() {
        when(problemLoader.getProblemsForTag("binary-trees")).thenReturn(List.of());

        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagName("Binary Trees");
        tag.setTagSlug("binary-tree");

        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Invert Tree", "invert-tree", "Easy");
        when(problemLoader.getProblemsForTag("binary-tree")).thenReturn(List.of(p));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(80, List.of()));

        Optional<CandidatePoolService.Candidate> best = service.bestProblemForTopic(ctx(List.of(tag)), "Binary Trees");

        assertTrue(best.isPresent());
        assertEquals("invert-tree", best.get().problem().getTitleSlug());
    }

    @Test
    void bestProblemForTopicShouldReturnEmptyWhenNoMatch() {
        when(problemLoader.getProblemsForTag("binary-trees")).thenReturn(List.of());
        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagName("Arrays");
        tag.setTagSlug("arrays");

        Optional<CandidatePoolService.Candidate> best = service.bestProblemForTopic(ctx(List.of(tag)), "Binary Trees");

        assertTrue(best.isEmpty());
    }

    @Test
    void shouldNeverBuildScoringContextItself() {
        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy");
        when(problemLoader.getProblemsForTag("arrays")).thenReturn(List.of(p));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(60, List.of()));

        service.bestProblem(ctx(List.of()), null, "arrays");

        verify(problemScorer, never()).context(any());
    }

    @Test
    void sameContextScoresEachTagOnlyOnceAcrossPicks() {
        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagSlug("dp");
        tag.setProblemsSolved(2);

        ProblemLoader.ProblemEntry p1 = new ProblemLoader.ProblemEntry("Coin Change", "coin-change", "Medium");
        ProblemLoader.ProblemEntry p2 = new ProblemLoader.ProblemEntry("House Robber", "house-robber", "Medium");
        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of(p1, p2));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(70, List.of()));

        ProblemScorer.ScoringContext ctx = ctx(List.of(tag));

        service.bestProblem(ctx, null, "dp");
        service.bestProblem(ctx, null, "dp");
        service.bestProblemForTopic(ctx, "DP");

        verify(problemScorer, times(2)).breakdown(any(), any(), eq("dp"));
    }

    @Test
    void rankForUserShouldComposeSuggestionsAndWeakTagsDeduped() {
        LeetCodeTagStat weak = new LeetCodeTagStat();
        weak.setTagSlug("dp");
        weak.setProblemsSolved(2);

        ProblemSuggestion suggestion = new ProblemSuggestion();
        suggestion.setSource("RECOMMENDATION");
        suggestion.setTitle("Suggested One");
        suggestion.setTitleSlug("sug1");
        suggestion.setDifficulty("Medium");
        suggestion.setTopicTagSlug("dp");

        ProblemLoader.ProblemEntry dup = new ProblemLoader.ProblemEntry("Suggested One", "sug1", "Medium");
        ProblemLoader.ProblemEntry other = new ProblemLoader.ProblemEntry("Other", "other", "Easy");
        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of(dup, other));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(70, List.of()));

        List<CandidatePoolService.Candidate> ranked = service.rankForUser(ctx(List.of(weak), List.of(suggestion)), 150);

        assertEquals(2, ranked.size());
        assertEquals(1, ranked.stream().filter(c -> c.problem().getTitleSlug().equals("sug1")).count());
        assertTrue(ranked.stream().anyMatch(c -> c.problem().getTitleSlug().equals("other")));
    }

    @Test
    void rankForUserShouldCapSuggestionsAtHalfThePool() {
        LeetCodeTagStat weak = new LeetCodeTagStat();
        weak.setTagSlug("dp");
        weak.setProblemsSolved(2);

        List<ProblemSuggestion> suggestions = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ProblemSuggestion s = new ProblemSuggestion();
            s.setSource("RECOMMENDATION");
            s.setTitle("Suggested " + i);
            s.setTitleSlug("sug" + i);
            s.setDifficulty("Medium");
            s.setTopicTagSlug("dp");
            suggestions.add(s);
        }

        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of(
                new ProblemLoader.ProblemEntry("Tag One", "tag1", "Easy"),
                new ProblemLoader.ProblemEntry("Tag Two", "tag2", "Easy")));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(70, List.of()));

        List<CandidatePoolService.Candidate> ranked =
                service.rankForUser(ctx(List.of(weak), suggestions), 4);

        assertEquals(4, ranked.size());
        long fromSuggestions = ranked.stream().filter(c -> c.problem().getTitleSlug().startsWith("sug")).count();
        long fromTags = ranked.stream().filter(c -> c.problem().getTitleSlug().startsWith("tag")).count();
        assertEquals(2, fromSuggestions, "stale suggestion backlog must not crowd out fresh tag candidates");
        assertEquals(2, fromTags);
    }

    @Test
    void tagSlugForTopicShouldFuzzyMatchTagName() {
        LeetCodeTagStat tag = new LeetCodeTagStat();
        tag.setTagName("Sliding Window");
        tag.setTagSlug("sliding-window");

        String slug = service.tagSlugForTopic(ctx(List.of(tag)), "sliding-window drills");

        assertEquals("sliding-window", slug);
    }

    @Test
    void rankForUserShouldIgnoreNonRecommendationSuggestions() {
        ProblemSuggestion weakTagSuggestion = new ProblemSuggestion();
        weakTagSuggestion.setSource("WEAK_TAG");
        weakTagSuggestion.setTitleSlug("wts");

        LeetCodeTagStat weak = new LeetCodeTagStat();
        weak.setTagSlug("dp");
        weak.setProblemsSolved(2);
        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of());

        List<CandidatePoolService.Candidate> ranked = service.rankForUser(ctx(List.of(weak), List.of(weakTagSuggestion)), 150);

        assertTrue(ranked.isEmpty());
    }

    @Test
    void rankForUserShouldFallBackToStarterTagsWhenNoStats() {
        ProblemLoader.ProblemEntry p = new ProblemLoader.ProblemEntry("Two Sum", "two-sum", "Easy");
        when(problemLoader.getProblemsForTag(anyString())).thenReturn(List.of());
        when(problemLoader.getProblemsForTag("array")).thenReturn(List.of(p));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(60, List.of()));

        List<CandidatePoolService.Candidate> ranked = service.rankForUser(ctx(List.of()), 150);

        assertEquals(1, ranked.size());
        assertEquals("two-sum", ranked.getFirst().problem().getTitleSlug());
        assertEquals("array", ranked.getFirst().tagSlug());
    }

    @Test
    void rankForUserShouldCapAtRequestedLimit() {
        LeetCodeTagStat weak = new LeetCodeTagStat();
        weak.setTagSlug("dp");
        weak.setProblemsSolved(2);
        when(problemLoader.getProblemsForTag("dp")).thenReturn(List.of(
                new ProblemLoader.ProblemEntry("P1", "p1", "Medium"),
                new ProblemLoader.ProblemEntry("P2", "p2", "Medium"),
                new ProblemLoader.ProblemEntry("P3", "p3", "Medium")));
        when(problemScorer.breakdown(any(), any(), any())).thenReturn(new ProblemScorer.ScoreBreakdown(70, List.of()));

        List<CandidatePoolService.Candidate> ranked = service.rankForUser(ctx(List.of(weak)), 2);

        assertEquals(2, ranked.size());
    }
}
