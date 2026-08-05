package com.forge.recommendation.service;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CandidatePoolService {

    public record Candidate(ProblemLoader.ProblemEntry problem, String tagSlug, int score,
                            ProblemScorer.ScoreBreakdown breakdown) {}

    private final ProblemLoader problemLoader;
    private final ProblemScorer problemScorer;
    private final LeetCodeTagStatRepository tagStatRepository;

    public Optional<Candidate> bestProblem(UUID userId, String difficulty, String tagSlug) {
        List<String> candidateTags = tagSlug != null
                ? List.of(tagSlug)
                : allTagSlugsFallback(userId);
        return rank(userId, candidateTags, difficulty, 1).stream().findFirst();
    }

    public Optional<Candidate> bestProblemForTopic(UUID userId, String topicTitle) {
        String topicSlug = slugify(topicTitle);
        List<Candidate> ranked = rank(userId, List.of(topicSlug), null, 1);
        if (ranked.isEmpty()) {
            String matchingSlug = tagSlugForTopic(userId, topicTitle, topicSlug);
            if (matchingSlug != null && !matchingSlug.equals(topicSlug)) {
                ranked = rank(userId, List.of(matchingSlug), null, 1);
            }
        }
        return ranked.stream().findFirst();
    }

    public List<Candidate> rank(UUID userId, List<String> tagSlugs, String difficulty, int limit) {
        ProblemScorer.ScoringContext ctx = problemScorer.context(userId);
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

    public List<String> weakTagSlugs(UUID userId) {
        return tagStatRepository.findByUserId(userId).stream()
                .filter(ts -> ts.getProblemsSolved() != null && ts.getProblemsSolved() < 5)
                .map(LeetCodeTagStat::getTagSlug)
                .toList();
    }

    public List<String> allTagSlugsFallback(UUID userId) {
        List<String> weak = weakTagSlugs(userId);
        if (!weak.isEmpty()) return weak;
        return tagStatRepository.findByUserId(userId).stream()
                .map(LeetCodeTagStat::getTagSlug)
                .toList();
    }

    public String tagSlugForTopic(UUID userId, String topicTitle, String topicSlug) {
        return tagStatRepository.findByUserId(userId).stream()
                .filter(ts -> ts.getTagName().equalsIgnoreCase(topicTitle)
                        || ts.getTagSlug().equalsIgnoreCase(topicSlug))
                .findFirst()
                .map(LeetCodeTagStat::getTagSlug)
                .orElse(null);
    }

    private String slugify(String topicTitle) {
        return topicTitle.toLowerCase().replace(' ', '-').replaceAll("[^a-z0-9-]", "");
    }
}
