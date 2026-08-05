package com.forge.recommendation.service;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CandidatePoolService {

    public static final int MAX_CANDIDATES = 150;
    private static final List<String> STARTER_TAGS = List.of("array", "hash-table", "two-pointers", "string", "binary-search");

    public record Candidate(ProblemLoader.ProblemEntry problem, String tagSlug, int score,
                            ProblemScorer.ScoreBreakdown breakdown) {}

    private final ProblemLoader problemLoader;
    private final ProblemScorer problemScorer;

    public List<Candidate> rankForUser(ProblemScorer.ScoringContext ctx, int cap) {
        List<Candidate> scored = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ProblemSuggestion ps : ctx.suggestions()) {
            if (!"RECOMMENDATION".equals(ps.getSource())) continue;
            if (scored.size() >= cap) break;
            if (seen.add(ps.getTitleSlug())) {
                ProblemLoader.ProblemEntry entry = new ProblemLoader.ProblemEntry(ps.getTitle(), ps.getTitleSlug(), ps.getDifficulty());
                ProblemScorer.ScoreBreakdown breakdown = problemScorer.breakdown(ctx, entry, ps.getTopicTagSlug());
                scored.add(new Candidate(entry, ps.getTopicTagSlug(), breakdown.total(), breakdown));
            }
        }

        for (String tagSlug : resolveCandidateTags(ctx)) {
            if (scored.size() >= cap) break;
            for (ProblemLoader.ProblemEntry candidate : problemLoader.getProblemsForTag(tagSlug)) {
                if (scored.size() >= cap) break;
                if (!seen.add(candidate.getTitleSlug())) continue;
                ProblemScorer.ScoreBreakdown breakdown = problemScorer.breakdown(ctx, candidate, tagSlug);
                scored.add(new Candidate(candidate, tagSlug, breakdown.total(), breakdown));
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
            String matchingSlug = tagSlugForTopic(ctx, topicTitle, topicSlug);
            if (matchingSlug != null && !matchingSlug.equals(topicSlug)) {
                ranked = rank(ctx, List.of(matchingSlug), null, 1);
            }
        }
        return ranked.stream().findFirst();
    }

    public List<Candidate> rank(ProblemScorer.ScoringContext ctx, List<String> tagSlugs, String difficulty, int limit) {
        List<Candidate> scored = new ArrayList<>();
        for (String tag : tagSlugs) {
            for (ProblemLoader.ProblemEntry candidate : problemLoader.getProblemsForTag(tag)) {
                if (difficulty != null && !candidate.getDifficulty().equalsIgnoreCase(difficulty)) continue;
                ProblemScorer.ScoreBreakdown breakdown = problemScorer.breakdown(ctx, candidate, tag);
                scored.add(new Candidate(candidate, tag, breakdown.total(), breakdown));
            }
        }
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored.stream().limit(Math.max(1, limit)).toList();
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

    public String tagSlugForTopic(ProblemScorer.ScoringContext ctx, String topicTitle, String topicSlug) {
        return ctx.stats().stream()
                .filter(ts -> ts.getTagName().equalsIgnoreCase(topicTitle)
                        || ts.getTagSlug().equalsIgnoreCase(topicSlug))
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
}
