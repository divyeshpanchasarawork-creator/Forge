package com.forge.recommendation.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.ProblemLoader;
import com.forge.common.util.ProblemScorer;
import com.forge.common.util.ReadinessCalculator;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.entity.ProblemSuggestion;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.leetcode.repository.ProblemSuggestionRepository;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.repository.RecommendationRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final TopicRepository topicRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;
    private final LeetCodeTagStatRepository tagStatRepository;
    private final ProblemSuggestionRepository problemSuggestionRepository;
    private final ProblemLoader problemLoader;
    private final ProblemScorer problemScorer;

    public List<Recommendation> generateForUser(UUID userId, boolean persist) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Recommendation> recs = new ArrayList<>();
        recs.addAll(checkLowConfidenceTopics(userId, user));
        recs.addAll(checkOverdueRevisions(userId, user));

        LeetCodeSnapshot snapshot = snapshotRepository.findByUserId(userId).orElse(null);
        int streak = (snapshot != null && snapshot.getStreak() != null) ? snapshot.getStreak() : 0;

        if (snapshot != null) {
            recs.addAll(generateLcRecommendations(userId, snapshot));
            recs.addAll(checkNextMilestone(snapshot));
            recs.addAll(checkDifficultyGap(snapshot, userId, user));
        } else if (user.getLeetcodeUsername() != null && !user.getLeetcodeUsername().isBlank()) {
            recs.add(createRecommendation(
                    "Connect your LeetCode profile",
                    "You have a LeetCode username set but haven't synced yet. Sync to get personalized insights.",
                    "Sync your LeetCode profile to unlock data-driven recommendations.",
                    scorePriority(2, streak), "SYNC_LEETCODE", user));
        }

        recs.forEach(r -> r.setUser(user));

        List<Recommendation> sorted = recs.stream()
                .sorted(Comparator.comparing(Recommendation::getPriority))
                .toList();

        if (persist) {
            recommendationRepository.deleteByUserIdAndDismissed(userId, false);
            recommendationRepository.saveAll(sorted);
            syncRecProblemsToSuggestions(userId, user);
            log.info("Generated and saved {} recommendations for user {}", sorted.size(), userId);
        }

        return sorted;
    }

    public List<Recommendation> generateForUser(UUID userId) {
        return generateForUser(userId, false);
    }

    private void syncRecProblemsToSuggestions(UUID userId, User user) {
        List<Recommendation> recsWithProblems = recommendationRepository
                .findByUserIdAndDismissedOrderByPriorityAscCreatedAtDesc(userId, false)
                .stream()
                .filter(r -> r.getProblemSlug() != null)
                .toList();

        if (recsWithProblems.isEmpty()) return;

        Set<String> existingSlugs = new HashSet<>();
        problemSuggestionRepository.findByUserId(userId)
                .forEach(ps -> existingSlugs.add(ps.getTitleSlug()));

        List<ProblemSuggestion> toSave = new ArrayList<>();
        for (Recommendation rec : recsWithProblems) {
            if (!existingSlugs.contains(rec.getProblemSlug())) {
                ProblemSuggestion suggestion = new ProblemSuggestion();
                suggestion.setUser(user);
                suggestion.setTitle(rec.getProblemTitle());
                suggestion.setTitleSlug(rec.getProblemSlug());
                suggestion.setDifficulty(rec.getProblemDifficulty());
                suggestion.setSource("RECOMMENDATION");
                toSave.add(suggestion);
                existingSlugs.add(rec.getProblemSlug());
            }
        }

        if (!toSave.isEmpty()) {
            problemSuggestionRepository.saveAll(toSave);
            log.info("Synced {} recommendation-linked problems to suggestions for user {}", toSave.size(), userId);
        }
    }

    private int scorePriority(int basePriority, int streak) {
        int streakPenalty = (streak < 3) ? 2 : 0;
        return basePriority + streakPenalty;
    }

    private List<Recommendation> checkDifficultyGap(LeetCodeSnapshot snapshot, UUID userId, User user) {
        List<Recommendation> recs = new ArrayList<>();
        int target = user.getTargetLevel() != null ? user.getTargetLevel() : 5;

        if (snapshot.getMediumSolved() == null) return recs;
        int total = snapshot.getTotalSolved() != null ? snapshot.getTotalSolved() : 0;
        int easy = snapshot.getEasySolved() != null ? snapshot.getEasySolved() : 0;
        int medium = snapshot.getMediumSolved() != null ? snapshot.getMediumSolved() : 0;
        int hard = snapshot.getHardSolved() != null ? snapshot.getHardSolved() : 0;

        int targetTotal = ReadinessCalculator.getTargetTotal(target);
        int targetHardPct = ReadinessCalculator.getTargetHardPct(target);
        int targetMediumPct = ReadinessCalculator.getTargetMediumPct(target);

        if (total < targetTotal) {
            recs.add(createRecommendation(
                    "Solve " + (targetTotal - total) + " more problems for Level " + target,
                    "You've solved " + total + " / " + targetTotal + " problems. " + (targetTotal - total) + " more to go.",
                    "Your target level " + target + " requires at least " + targetTotal + " problems. Stay consistent.",
                    total > targetTotal / 2 ? 3 : 2, "MILESTONE", user));
        }

        int idealHard = (targetHardPct * targetTotal) / 100;
        if (hard < idealHard / 2) {
            ProblemLoader.ProblemEntry bestHard = pickBestProblem(userId, "Hard", null);
            if (bestHard != null) {
                recs.add(createRecommendation(
                        "Try " + bestHard.getTitle() + " (Hard)",
                        "You've solved " + hard + " Hard problems. At Level " + target + ", aim for " + idealHard + ".",
                        "Hard problems are weighted heavily at your target level.",
                        easy > hard ? 1 : 2, "TRY_HARD", user)
                        .withProblem(bestHard.getTitle(), bestHard.getTitleSlug(), "Hard"));
            } else {
                recs.add(createRecommendation(
                        "Focus on Hard problems for Level " + target,
                        "You've solved " + hard + " Hard problems. At Level " + target + ", aim for " + idealHard + ".",
                        "Hard problems are weighted heavily at your target level. Practice them regularly.",
                        easy > hard ? 1 : 2, "TRY_HARD", user));
            }
        }

        int idealMedium = (targetMediumPct * targetTotal) / 100;
        if (medium < idealMedium / 2) {
            recs.add(createRecommendation(
                    "Build your Medium count for Level " + target,
                    "You've solved " + medium + " Medium problems. Target: ~" + idealMedium + ".",
                    "Medium problems form the backbone of most interviews at Level " + target + ".",
                    medium < idealMedium / 3 ? 1 : 2, "LEVEL_UP", user));
        }

        return recs;
    }

    private List<Recommendation> checkNextMilestone(LeetCodeSnapshot snapshot) {
        List<Recommendation> recs = new ArrayList<>();

        int easy = snapshot.getEasySolved() != null ? snapshot.getEasySolved() : 0;
        int medium = snapshot.getMediumSolved() != null ? snapshot.getMediumSolved() : 0;
        int hard = snapshot.getHardSolved() != null ? snapshot.getHardSolved() : 0;
        int total = snapshot.getTotalSolved() != null ? snapshot.getTotalSolved() : 0;

        int nextRoundMilestone = ((total / 50) + 1) * 50;
        int toGo = nextRoundMilestone - total;
        if (toGo > 0 && toGo <= 20) {
            recs.add(createRecommendation(
                    "Reach " + nextRoundMilestone + " problems solved",
                    "You're only " + toGo + " problems away from " + nextRoundMilestone + " total solved!",
                    "Milestones build momentum. Set a short-term goal to close the gap.",
                    2, "MILESTONE", null));
        }

        int nextMediumMilestone = ((medium / 25) + 1) * 25;
        int mediumToGo = nextMediumMilestone - medium;
        if (mediumToGo > 0 && mediumToGo <= 10) {
            recs.add(createRecommendation(
                    "Solve " + mediumToGo + " more Medium problems",
                    mediumToGo + " more Medium problems will get you to " + nextMediumMilestone + ".",
                    "Medium problems are the sweet spot for interview preparation.",
                    2, "MILESTONE", null));
        }

        if (easy > 0 && medium == 0 && hard == 0) {
            recs.add(createRecommendation(
                    "Start solving harder problems",
                    "All your solved problems are Easy. Challenge yourself with Medium and Hard.",
                    "Progressive difficulty builds depth. Try a Medium problem next.",
                    2, "LEVEL_UP", null));
        }

        if (easy > 20 && (medium == 0 || medium < easy / 3)) {
            recs.add(createRecommendation(
                    "Transition from Easy to Medium",
                    "You've built a solid Easy foundation. Now focus on Medium problems.",
                    "Interviews at most companies go beyond Easy. Medium problems are the next step.",
                    2, "LEVEL_UP", null));
        }

        return recs;
    }

    private List<Recommendation> generateLcRecommendations(UUID userId, LeetCodeSnapshot snapshot) {
        List<Recommendation> recs = new ArrayList<>();
        User user = userRepository.getReferenceById(userId);

        List<LeetCodeTagStat> weakTags = tagStatRepository.findByUserId(userId).stream()
                .filter(ts -> ts.getProblemsSolved() < 5 && ts.getProblemsSolved() > 0)
                .toList();

        List<ProblemSuggestion> suggestions = problemSuggestionRepository.findByUserId(userId);

        for (LeetCodeTagStat tag : weakTags) {
            ProblemLoader.ProblemEntry best = pickBestProblem(userId, null, tag.getTagSlug());
            if (best != null) {
                recs.add(createRecommendation(
                        "Try " + best.getTitle() + " (" + best.getDifficulty() + ")",
                        "You've only solved " + tag.getProblemsSolved() + " problem(s) in " + tag.getTagName() + ".",
                        "Building breadth across tags strengthens your problem-solving toolkit.",
                        tag.getProblemsSolved() <= 2 ? 1 : 2, "PRACTICE_TAG", user)
                        .withProblem(best.getTitle(), best.getTitleSlug(), best.getDifficulty()));
            } else {
                List<ProblemSuggestion> tagSuggestions = suggestions.stream()
                        .filter(s -> tag.getTagSlug().equals(s.getTopicTagSlug()))
                        .limit(2)
                        .toList();
                if (!tagSuggestions.isEmpty()) {
                    for (ProblemSuggestion ps : tagSuggestions) {
                        recs.add(createRecommendation(
                                "Try " + ps.getTitle() + " (" + ps.getDifficulty() + ")",
                                "You've only solved " + tag.getProblemsSolved() + " problem(s) in " + tag.getTagName() + ".",
                                "Building breadth across tags strengthens your problem-solving toolkit.",
                                tag.getProblemsSolved() <= 2 ? 1 : 2, "PRACTICE_TAG", user)
                                .withProblem(ps.getTitle(), ps.getTitleSlug(), ps.getDifficulty()));
                    }
                } else {
                    recs.add(createRecommendation(
                            "Practice " + tag.getTagName(),
                            "You've only solved " + tag.getProblemsSolved() + " problem(s) in " + tag.getTagName() + ".",
                            "Building breadth across tags strengthens your problem-solving toolkit.",
                            tag.getProblemsSolved() <= 2 ? 1 : 2, "PRACTICE_TAG", user));
                }
            }
        }

        if (snapshot.getEasySolved() > 0) {
            double easyRatio = (double) snapshot.getEasySolved() / snapshot.getTotalSolved();
            if (easyRatio > 0.6 && snapshot.getMediumSolved() < snapshot.getEasySolved() / 2) {
                recs.add(createRecommendation(
                        "Level up to Medium difficulty",
                        "Over 60% of your solved problems are Easy. You're ready for more challenge.",
                        "Moving to Medium problems accelerates growth. Try 1 Medium for every 2 Easy you solve.",
                        2, "LEVEL_UP", user));
            }
        }

        if (snapshot.getHardSolved() == 0 && snapshot.getMediumSolved() >= 10) {
            ProblemLoader.ProblemEntry bestHard = pickBestProblem(userId, "Hard", null);
            if (bestHard != null) {
                recs.add(createRecommendation(
                        "Start with " + bestHard.getTitle() + " (Hard)",
                        "With " + snapshot.getMediumSolved() + " Medium problems solved, you're ready for Hard.",
                        "Hard problems build deep algorithmic thinking.",
                        2, "TRY_HARD", user)
                        .withProblem(bestHard.getTitle(), bestHard.getTitleSlug(), "Hard"));
            } else {
                recs.add(createRecommendation(
                        "Start tackling Hard problems",
                        "With " + snapshot.getMediumSolved() + " Medium problems solved, you're ready for Hard.",
                        "Hard problems build deep algorithmic thinking. Start with popular tagged Hards.",
                        2, "TRY_HARD", user));
            }
        }

        if (snapshot.getStreak() > 0) {
            recs.add(createRecommendation(
                    "Maintain your " + snapshot.getStreak() + "-day streak",
                    "You've been consistent! Don't break the chain.",
                    "Consistency compounds. Even solving 1 problem a day keeps the streak alive.",
                    3, "MAINTAIN_STREAK", user));
        } else if (snapshot.getTotalActiveDays() > 0) {
            recs.add(createRecommendation(
                    "Start a new solving streak",
                    "Your streak is at 0. Begin a fresh streak today!",
                    "Daily practice builds momentum. Try to solve at least one problem today.",
                    2, "START_STREAK", user));
        }

        if (snapshot.getContestAttendedCount() == null || snapshot.getContestAttendedCount() == 0) {
            recs.add(createRecommendation(
                    "Try LeetCode Weekly Contests",
                    "You haven't attended any contests yet. They're great for timed practice.",
                    "Contests simulate interview pressure and expose you to diverse problem types.",
                    3, "TRY_CONTEST", user));
        }

        return recs;
    }

    private ProblemLoader.ProblemEntry pickBestProblem(UUID userId, String difficulty, String tagSlug) {
        List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);
        List<String> candidateTags;
        if (tagSlug != null) {
            candidateTags = List.of(tagSlug);
        } else {
            candidateTags = tagStats.stream()
                    .filter(ts -> ts.getProblemsSolved() != null && ts.getProblemsSolved() < 5)
                    .map(LeetCodeTagStat::getTagSlug)
                    .toList();
            if (candidateTags.isEmpty()) {
                candidateTags = tagStats.stream()
                        .map(LeetCodeTagStat::getTagSlug)
                        .toList();
            }
        }

        List<ProblemScorer.ScoredProblem> scored = new ArrayList<>();
        for (String ct : candidateTags) {
            List<ProblemLoader.ProblemEntry> candidates = problemLoader.getProblemsForTag(ct);
            for (ProblemLoader.ProblemEntry c : candidates) {
                if (difficulty != null && !c.getDifficulty().equalsIgnoreCase(difficulty)) continue;
                int score = problemScorer.score(userId, c, ct);
                scored.add(new ProblemScorer.ScoredProblem(c, ct, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored.isEmpty() ? null : scored.getFirst().problem();
    }

    private List<Recommendation> checkLowConfidenceTopics(UUID userId, User user) {
        List<Topic> weakTopics = topicRepository.findWeakTopicsByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();
        int target = user.getTargetLevel() != null ? user.getTargetLevel() : 5;

        for (Topic topic : weakTopics) {
            int adjustedPriority = target >= 7 ? 1 : (target >= 4 ? 2 : 3);
            ProblemLoader.ProblemEntry best = pickBestProblemForTopic(userId, topic.getTitle());
            if (best != null) {
                recs.add(createRecommendation(
                        "Review " + topic.getTitle() + " with " + best.getTitle(),
                        "Your confidence in " + topic.getTitle() + " is only " + topic.getConfidence() + "/10.",
                        "Low confidence indicates weak understanding. Practice with a curated problem.",
                        adjustedPriority, "REVIEW", user)
                        .withProblem(best.getTitle(), best.getTitleSlug(), best.getDifficulty()));
            } else {
                recs.add(createRecommendation(
                        "Review " + topic.getTitle(),
                        "Your confidence in " + topic.getTitle() + " is only " + topic.getConfidence() + "/10.",
                        "Low confidence indicates weak understanding. Regular review helps build mastery.",
                        adjustedPriority, "REVIEW", user));
            }
        }

        if (target >= 7) {
            List<Topic> midTopics = topicRepository.findByUserId(userId,
                            org.springframework.data.domain.PageRequest.of(0, 50)).getContent().stream()
                    .filter(t -> t.getConfidence() >= 4 && t.getConfidence() < 7)
                    .toList();
            for (Topic topic : midTopics) {
                ProblemLoader.ProblemEntry best = pickBestProblemForTopic(userId, topic.getTitle());
                if (best != null) {
                    recs.add(createRecommendation(
                            "Deepen " + topic.getTitle() + " with " + best.getTitle(),
                            topic.getTitle() + " confidence is " + topic.getConfidence() + "/10. At your target level, aim for 7+.",
                            "For Level " + target + " companies, medium confidence isn't enough. Push for mastery.",
                            2, "REVIEW", user)
                            .withProblem(best.getTitle(), best.getTitleSlug(), best.getDifficulty()));
                } else {
                    recs.add(createRecommendation(
                            "Deepen " + topic.getTitle(),
                            topic.getTitle() + " confidence is " + topic.getConfidence() + "/10. At your target level, aim for 7+.",
                            "For Level " + target + " companies, medium confidence isn't enough. Push for mastery.",
                            2, "REVIEW", user));
                }
            }
        }

        return recs;
    }

    private ProblemLoader.ProblemEntry pickBestProblemForTopic(UUID userId, String topicTitle) {
        String topicSlug = topicTitle.toLowerCase().replace(' ', '-').replaceAll("[^a-z0-9-]", "");
        List<ProblemLoader.ProblemEntry> candidates = problemLoader.getProblemsForTag(topicSlug);
        if (candidates.isEmpty()) {
            List<LeetCodeTagStat> tagStats = tagStatRepository.findByUserId(userId);
            String matchingSlug = tagStats.stream()
                    .filter(ts -> ts.getTagName().equalsIgnoreCase(topicTitle)
                            || ts.getTagSlug().equalsIgnoreCase(topicSlug))
                    .findFirst().map(LeetCodeTagStat::getTagSlug)
                    .orElse(null);
            if (matchingSlug != null) {
                candidates = problemLoader.getProblemsForTag(matchingSlug);
            }
        }
        if (candidates.isEmpty()) return null;

        List<ProblemScorer.ScoredProblem> scored = candidates.stream()
                .map(c -> new ProblemScorer.ScoredProblem(c, topicSlug, problemScorer.score(userId, c, topicSlug)))
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();

        return scored.isEmpty() ? null : scored.getFirst().problem();
    }

    private List<Recommendation> checkOverdueRevisions(UUID userId, User user) {
        List<Topic> overdueTopics = topicRepository.findTopicsNeedingRevisionByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();
        int target = user.getTargetLevel() != null ? user.getTargetLevel() : 5;
        int overdueThreshold = target >= 7 ? 7 : (target >= 4 ? 14 : 21);

        for (Topic topic : overdueTopics) {
            if (topic.getLastRevision() != null) {
                long daysSince = Duration.between(topic.getLastRevision(), LocalDateTime.now()).toDays();
                if (daysSince > overdueThreshold) {
                    ProblemLoader.ProblemEntry best = pickBestProblemForTopic(userId, topic.getTitle());
                    if (best != null) {
                        recs.add(createRecommendation(
                                topic.getTitle() + " needs review via " + best.getTitle(),
                                topic.getTitle() + " hasn't been reviewed in " + daysSince + " days.",
                                "Spaced repetition requires regular review. Reinforce with a practice problem.",
                                1, "REVIEW", user)
                                .withProblem(best.getTitle(), best.getTitleSlug(), best.getDifficulty()));
                    } else {
                        recs.add(createRecommendation(
                                topic.getTitle() + " needs review",
                                topic.getTitle() + " hasn't been reviewed in " + daysSince + " days.",
                                "Spaced repetition requires regular review. Your retention drops without practice.",
                                1, "REVIEW", user));
                    }
                }
            }
        }

        return recs;
    }

    private Recommendation createRecommendation(String title, String description, String reason, int priority, String action, User user) {
        Recommendation rec = new Recommendation();
        rec.setTitle(title);
        rec.setDescription(description);
        rec.setReason(reason);
        rec.setPriority(priority);
        rec.setAction(action);
        rec.setDismissed(false);
        rec.setUser(user);
        return rec;
    }
}
