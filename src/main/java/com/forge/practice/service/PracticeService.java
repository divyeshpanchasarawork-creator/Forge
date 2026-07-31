package com.forge.practice.service;

import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.SecurityUtils;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.practice.dto.PracticeProblemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private final LeetCodeTagStatRepository tagStatRepository;
    private final ProblemLoader problemLoader;
    private final ProblemScorer problemScorer;
    private final ProblemSuggestionRepository problemSuggestionRepository;

    public List<PracticeProblemResponse> getPracticeQueue() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<PracticeProblemResponse> queue = new ArrayList<>();
        Set<String> usedSlugs = new HashSet<>();

        List<ProblemSuggestion> recSuggestions = problemSuggestionRepository.findByUserId(userId).stream()
                .filter(ps -> "RECOMMENDATION".equals(ps.getSource()))
                .toList();

        for (ProblemSuggestion ps : recSuggestions) {
            if (queue.size() >= 10) break;
            if (usedSlugs.add(ps.getTitleSlug())) {
                queue.add(new PracticeProblemResponse(
                        ps.getTitle(), ps.getTitleSlug(), ps.getDifficulty(),
                        null, "Recommended by your personalized plan."));
            }
        }

        List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);
        if (tagStats.isEmpty()) {
            List<ProblemLoader.ProblemEntry> all = new ArrayList<>(problemLoader.getAllProblems());
            Collections.shuffle(all, new Random());
            for (ProblemLoader.ProblemEntry p : all) {
                if (queue.size() >= 10) break;
                if (usedSlugs.add(p.getTitleSlug())) {
                    queue.add(new PracticeProblemResponse(
                            p.getTitle(), p.getTitleSlug(), p.getDifficulty(),
                            null, "Popular problem to build general problem-solving skills."));
                }
            }
            return queue;
        }

        List<String> weakTagSlugs = tagStats.stream()
                .filter(ts -> ts.getProblemsSolved() == null || ts.getProblemsSolved() < 5)
                .map(LeetCodeTagStat::getTagSlug)
                .filter(slug -> problemLoader.getProblemsForTag(slug) != null
                        && !problemLoader.getProblemsForTag(slug).isEmpty())
                .toList();

        if (!weakTagSlugs.isEmpty()) {
            List<ProblemScorer.ScoredProblem> scored = new ArrayList<>();
            for (String tagSlug : weakTagSlugs) {
                String tagName = tagStats.stream()
                        .filter(ts -> ts.getTagSlug().equals(tagSlug))
                        .findFirst().map(LeetCodeTagStat::getTagName)
                        .orElse(tagSlug);
                List<ProblemLoader.ProblemEntry> candidates = problemLoader.getProblemsForTag(tagSlug);
                for (ProblemLoader.ProblemEntry candidate : candidates) {
                    int score = problemScorer.score(userId, candidate, tagSlug);
                    scored.add(new ProblemScorer.ScoredProblem(candidate, tagSlug, score));
                }
            }

            scored.sort((a, b) -> Integer.compare(b.score(), a.score()));

            for (ProblemScorer.ScoredProblem sp : scored) {
                if (queue.size() >= 10) break;
                if (usedSlugs.add(sp.problem().getTitleSlug())) {
                    String tagName = tagStats.stream()
                            .filter(ts -> ts.getTagSlug().equals(sp.tagSlug()))
                            .findFirst().map(LeetCodeTagStat::getTagName)
                            .orElse(sp.tagSlug());
                    int solved = tagStats.stream()
                            .filter(ts -> ts.getTagSlug().equals(sp.tagSlug()))
                            .findFirst()
                            .map(ts -> ts.getProblemsSolved() != null ? ts.getProblemsSolved() : 0)
                            .orElse(0);
                    String reason;
                    if (solved == 0) {
                        reason = "You haven't solved any " + tagName + " problems yet. Start here.";
                    } else {
                        reason = "You've solved " + solved + " " + tagName + " problem(s). This will build breadth.";
                    }
                    queue.add(new PracticeProblemResponse(
                            sp.problem().getTitle(), sp.problem().getTitleSlug(), sp.problem().getDifficulty(),
                            tagName, reason));
                }
            }
        }

        if (queue.size() < 5) {
            List<ProblemLoader.ProblemEntry> all = new ArrayList<>(problemLoader.getAllProblems());
            Collections.shuffle(all, new Random());
            for (ProblemLoader.ProblemEntry p : all) {
                if (queue.size() >= 10) break;
                if (usedSlugs.add(p.getTitleSlug())) {
                    queue.add(new PracticeProblemResponse(
                            p.getTitle(), p.getTitleSlug(), p.getDifficulty(),
                            null, "Popular problem to build general problem-solving skills."));
                }
            }
        }

        return queue;
    }
}
