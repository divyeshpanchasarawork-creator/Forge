package com.forge.recommendation.service;

import com.forge.common.util.DifficultyUtil;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

@Component
@RequiredArgsConstructor
public class CandidatePoolService {

    public static final int MAX_CANDIDATES = 150;
    private static final List<String> STARTER_TAGS = List.of("array", "hash-table", "two-pointers", "string", "binary-search");

    public record Candidate(ProblemLoader.ProblemEntry problem, String tagSlug, int score,
                            ProblemScorer.ScoreBreakdown breakdown) {}

    private final ProblemLoader problemLoader;
    private final ProblemScorer problemScorer;

    /**
     * Per-generation scored-tag cache. {@link ProblemScorer.ScoringContext} is immutable for the
     * lifetime of one generation/queue build, so the breakdown of every (problem, tag) is
     * deterministic; each tag is scored at most once per context instead of once per pick call.
     * Keyed by identity (==) so two equal-but-distinct contexts never share stale rankings, and
     * held weakly so entries are reclaimed once the request drops the context.
     */
    private final Map<CtxKey, Map<String, List<Candidate>>> ctxTagCache = new WeakHashMap<>();

    public List<Candidate> rankForUser(ProblemScorer.ScoringContext ctx, int cap) {
        List<Candidate> scored = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Stored suggestions can accumulate a stale backlog; capping their share keeps
        // tag-derived candidates — which track current weak spots — in every generation.
        int suggestionShare = Math.max(1, cap / 2);

        for (ProblemSuggestion ps : ctx.suggestions()) {
            if (!"RECOMMENDATION".equals(ps.getSource())) continue;
            if (scored.size() >= suggestionShare || scored.size() >= cap) break;
            if (seen.add(ps.getTitleSlug())) {
                ProblemLoader.ProblemEntry entry = new ProblemLoader.ProblemEntry(ps.getTitle(), ps.getTitleSlug(),
                        DifficultyUtil.titleCase(ps.getDifficulty()));
                ProblemScorer.ScoreBreakdown breakdown = problemScorer.breakdown(ctx, entry, ps.getTopicTagSlug());
                scored.add(new Candidate(entry, ps.getTopicTagSlug(), breakdown.total(), breakdown));
            }
        }

        for (String tagSlug : resolveCandidateTags(ctx)) {
            for (Candidate candidate : scoredForTag(ctx, tagSlug)) {
                if (scored.size() >= cap) break;
                if (!seen.add(candidate.problem().getTitleSlug())) continue;
                scored.add(candidate);
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored.stream().limit(Math.max(0, cap)).toList();
    }

    public Optional<Candidate> bestProblem(ProblemScorer.ScoringContext ctx, String difficulty, String tagSlug) {
        List<String> candidateTags = tagSlug != null
                ? List.of(tagSlug)
                : allTagSlugsFallback(ctx);
        return rank(ctx, candidateTags, difficulty, 1).stream().findFirst();
    }

    public Optional<Candidate> bestProblemForTopic(ProblemScorer.ScoringContext ctx, String topicTitle) {
        String topicSlug = slugify(topicTitle);
        List<Candidate> ranked = rank(ctx, List.of(topicSlug), null, 1);
        if (ranked.isEmpty()) {
            String matchingSlug = tagSlugForTopic(ctx, topicTitle);
            if (matchingSlug != null && !matchingSlug.equals(topicSlug)) {
                ranked = rank(ctx, List.of(matchingSlug), null, 1);
            }
        }
        return ranked.stream().findFirst();
    }

    public List<Candidate> rank(ProblemScorer.ScoringContext ctx, List<String> tagSlugs, String difficulty, int limit) {
        List<Candidate> scored = new ArrayList<>();
        for (String tag : tagSlugs) {
            for (Candidate candidate : scoredForTag(ctx, tag)) {
                if (difficulty != null && !candidate.problem().getDifficulty().equalsIgnoreCase(difficulty)) continue;
                scored.add(candidate);
            }
        }
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored.stream().limit(Math.max(1, limit)).toList();
    }

    private synchronized List<Candidate> scoredForTag(ProblemScorer.ScoringContext ctx, String tagSlug) {
        Map<String, List<Candidate>> perTag = ctxTagCache.computeIfAbsent(new CtxKey(ctx), k -> new HashMap<>());
        return perTag.computeIfAbsent(tagSlug, slug -> scoreTag(ctx, slug));
    }

    private List<Candidate> scoreTag(ProblemScorer.ScoringContext ctx, String tagSlug) {
        List<Candidate> scored = new ArrayList<>();
        for (ProblemLoader.ProblemEntry candidate : problemLoader.getProblemsForTag(tagSlug)) {
            ProblemScorer.ScoreBreakdown breakdown = problemScorer.breakdown(ctx, candidate, tagSlug);
            scored.add(new Candidate(candidate, tagSlug, breakdown.total(), breakdown));
        }
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored;
    }

    public List<String> weakTagSlugs(ProblemScorer.ScoringContext ctx) {
        return ctx.stats().stream()
                .filter(ts -> ts.getProblemsSolved() != null && ts.getProblemsSolved() < 5)
                .map(LeetCodeTagStat::getTagSlug)
                .toList();
    }

    public List<String> allTagSlugsFallback(ProblemScorer.ScoringContext ctx) {
        List<String> weak = weakTagSlugs(ctx);
        if (!weak.isEmpty()) return weak;
        return ctx.stats().stream()
                .map(LeetCodeTagStat::getTagSlug)
                .toList();
    }

    /**
     * Resolves a topic title to a real LeetCode tag slug via fuzzy title matching over the
     * user's synced tag stats — the shared {@link TitleMatcher} contract, not exact equality.
     */
    public String tagSlugForTopic(ProblemScorer.ScoringContext ctx, String topicTitle) {
        return ctx.stats().stream()
                .filter(ts -> com.forge.common.util.TitleMatcher.topicMatches(ts.getTagName(), topicTitle)
                        || com.forge.common.util.TitleMatcher.topicMatches(ts.getTagSlug(), topicTitle))
                .findFirst()
                .map(LeetCodeTagStat::getTagSlug)
                .orElse(null);
    }

    private List<String> resolveCandidateTags(ProblemScorer.ScoringContext ctx) {
        List<LeetCodeTagStat> tagStats = ctx.stats();
        if (tagStats.isEmpty()) return STARTER_TAGS;
        List<String> weakTags = tagStats.stream()
                .filter(ts -> ts.getProblemsSolved() == null || ts.getProblemsSolved() < 5)
                .map(LeetCodeTagStat::getTagSlug)
                .filter(slug -> !problemLoader.getProblemsForTag(slug).isEmpty())
                .toList();
        if (!weakTags.isEmpty()) return weakTags;
        return tagStats.stream()
                .map(LeetCodeTagStat::getTagSlug)
                .toList();
    }

    private String slugify(String topicTitle) {
        return topicTitle.toLowerCase().replace(' ', '-').replaceAll("[^a-z0-9-]", "");
    }

    private static final class CtxKey {
        private final ProblemScorer.ScoringContext ctx;

        private CtxKey(ProblemScorer.ScoringContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CtxKey key && key.ctx == ctx;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(ctx);
        }
    }
}
