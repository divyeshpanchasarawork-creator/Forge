package com.forge.intelligence.service;

import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.auth.repository.UserRepository;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ColdStartService {

    private final TopicRepository topicRepository;
    private final ProblemAttemptRepository problemAttemptRepository;
    private final DailyMetricRepository dailyMetricRepository;
    private final RevisionRepository revisionRepository;
    private final UserRepository userRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;

    public enum Profile {
        BEGINNER, RETURNING, INTERMEDIATE, ADVANCED
    }

    public Profile classify(UUID userId) {
        List<Topic> topics = topicRepository.findByUserId(userId, PageRequest.of(0, 200));
        long attempts = problemAttemptRepository.countByUserIdAndAttemptedAtBetween(
                userId, LocalDateTime.now().minusYears(1), LocalDateTime.now());
        long distinctSolved = problemAttemptRepository.countDistinctProblemsByUserId(userId);
        double avgMastery = topics.isEmpty() ? 0 : topics.stream().mapToInt(Topic::getMastery).average().orElse(0);

        if (attempts == 0 && topics.isEmpty()) return Profile.BEGINNER;
        if (attempts == 0 && !topics.isEmpty() && avgMastery < 30) return Profile.RETURNING;
        if (distinctSolved >= 40 && avgMastery >= 60) return Profile.ADVANCED;
        if (distinctSolved >= 15 || avgMastery >= 45) return Profile.INTERMEDIATE;
        return Profile.RETURNING;
    }

    public boolean needsSeed(UUID userId) {
        return topicRepository.countByUserId(userId) == 0
                && snapshotRepository.findByUserId(userId).isEmpty();
    }

    public List<Topic> seedStarterTopics(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        List<Topic> existing = topicRepository.findByUserId(userId, PageRequest.of(0, 200));
        if (!existing.isEmpty()) return existing;

        List<Topic> seeds = List.of(
                starter("Arrays", "Indexed collection fundamentals — the foundation of most algorithms.", user),
                starter("Hash Table", "Key-value lookups for O(1) access patterns.", user),
                starter("Two Pointers", "Linear scans with two cursors for sorted/paired problems.", user),
                starter("String", "Manipulation and matching over character sequences.", user),
                starter("Binary Search", "Halving a search space on sorted or monotonic data.", user)
        );
        return topicRepository.saveAll(seeds);
    }

    private Topic starter(String title, String description, com.forge.auth.entity.User user) {
        Topic topic = new Topic();
        topic.setUser(user);
        topic.setTitle(title);
        topic.setDescription(description);
        topic.setCategory("Fundamentals");
        topic.setSource("COLD_START");
        topic.setConfidence(2);
        topic.setMastery(5);
        topic.setStatus("NOT_STARTED");
        return topic;
    }

    public String planMessage(UUID userId, Profile profile) {
        return switch (profile) {
            case BEGINNER -> "Fresh start. Build the fundamentals first — we seeded 5 core topics. Warm up with one easy problem, then reinforce a weak topic.";
            case RETURNING -> "Welcome back. Your foundation needs reinforcement — focus on topics with decaying retention before adding new material.";
            case INTERMEDIATE -> "Solid base detected. Push depth: challenge yourself with harder problems in your weakest tags while keeping revisions flowing.";
            case ADVANCED -> "You're operating at depth. The queue targets narrow gaps and overdue revisions to keep your edge sharp.";
        };
    }
}
