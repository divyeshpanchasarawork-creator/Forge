package com.forge.recommendation.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.entity.LeetCodeTagStat;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.leetcode.repository.LeetCodeTagStatRepository;
import com.forge.recommendation.entity.Recommendation;
import com.forge.recommendation.repository.RecommendationRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final TopicRepository topicRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;
    private final LeetCodeTagStatRepository tagStatRepository;

    public List<Recommendation> generateForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<Recommendation> recs = new ArrayList<>();
        recs.addAll(checkLowConfidenceTopics(userId));
        recs.addAll(checkOverdueRevisions(userId));
        recs.addAll(checkMasteryThreshold(userId));

        LeetCodeSnapshot snapshot = snapshotRepository.findByUserId(userId).orElse(null);
        if (snapshot != null) {
            recs.addAll(generateLcRecommendations(userId, snapshot));
        } else if (user.getLeetcodeUsername() != null && !user.getLeetcodeUsername().isBlank()) {
            recs.add(createRecommendation(
                    "Connect your LeetCode profile",
                    "You have a LeetCode username set but haven't synced yet. Sync to get personalized insights.",
                    "Sync your LeetCode profile to unlock data-driven recommendations.",
                    1, "SYNC_LEETCODE", user));
        }

        recs.forEach(r -> r.setUser(user));

        return recs.stream()
                .sorted(Comparator.comparing(Recommendation::getPriority))
                .toList();
    }

    private List<Recommendation> generateLcRecommendations(UUID userId, LeetCodeSnapshot snapshot) {
        List<Recommendation> recs = new ArrayList<>();
        User user = userRepository.getReferenceById(userId);

        List<LeetCodeTagStat> weakTags = tagStatRepository.findByUserId(userId).stream()
                .filter(ts -> ts.getProblemsSolved() < 5 && ts.getProblemsSolved() > 0)
                .toList();
        for (LeetCodeTagStat tag : weakTags) {
            recs.add(createRecommendation(
                    "Practice " + tag.getTagName(),
                    "You've only solved " + tag.getProblemsSolved() + " problem(s) in " + tag.getTagName() + ".",
                    "Building breadth across tags strengthens your problem-solving toolkit.",
                    1, "PRACTICE_TAG", user));
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
            recs.add(createRecommendation(
                    "Start tackling Hard problems",
                    "With " + snapshot.getMediumSolved() + " Medium problems solved, you're ready for Hard.",
                    "Hard problems build deep algorithmic thinking. Start with popular tagged Hards.",
                    2, "TRY_HARD", user));
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

    private List<Recommendation> checkLowConfidenceTopics(UUID userId) {
        List<Topic> weakTopics = topicRepository.findWeakTopicsByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();

        for (Topic topic : weakTopics) {
            recs.add(createRecommendation(
                    "Review " + topic.getTitle(),
                    "Your confidence in " + topic.getTitle() + " is only " + topic.getConfidence() + "/10.",
                    "Low confidence indicates weak understanding. Regular review helps build mastery.",
                    1, "REVIEW", userRepository.getReferenceById(userId)));
        }

        return recs;
    }

    private List<Recommendation> checkOverdueRevisions(UUID userId) {
        List<Topic> overdueTopics = topicRepository.findTopicsNeedingRevisionByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();
        User user = userRepository.getReferenceById(userId);

        for (Topic topic : overdueTopics) {
            if (topic.getLastRevision() != null) {
                long daysSince = java.time.Duration.between(topic.getLastRevision(), LocalDateTime.now()).toDays();
                if (daysSince > 14) {
                    recs.add(createRecommendation(
                            topic.getTitle() + " needs review",
                            topic.getTitle() + " hasn't been reviewed in " + daysSince + " days.",
                            "Spaced repetition requires regular review. Your retention drops without practice.",
                            1, "REVIEW", user));
                }
            }
        }

        return recs;
    }

    private List<Recommendation> checkMasteryThreshold(UUID userId) {
        List<Topic> strongTopics = topicRepository.findStrongTopicsByUserId(userId);
        List<Recommendation> recs = new ArrayList<>();
        User user = userRepository.getReferenceById(userId);

        for (Topic topic : strongTopics) {
            if (topic.getMastery() > 80) {
                recs.add(createRecommendation(
                        "Ready for advanced " + topic.getTitle(),
                        "Great progress on " + topic.getTitle() + "! Mastery: " + topic.getMastery() + "%",
                        "High mastery means you're ready to tackle more complex concepts in this area.",
                        3, "ADVANCE", user));
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
