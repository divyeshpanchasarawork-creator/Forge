package com.forge.analytics.service;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.ReadinessCalculator;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.entity.Journal;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RevisionRepository revisionRepository;
    private final JournalRepository journalRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;

    public AnalyticsResponse getAnalytics() {
        UUID userId = SecurityUtils.getCurrentUserId();

        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        List<Topic> weakTopics = topicRepository.findWeakTopicsByUserId(userId);
        List<Topic> strongTopics = topicRepository.findStrongTopicsByUserId(userId);

        long totalTopics = topicRepository.countByUserId(userId);

        LeetCodeSnapshot lcSnapshot = snapshotRepository.findByUserId(userId).orElse(null);
        long totalProblems = lcSnapshot != null ? lcSnapshot.getTotalSolved() : 0;

        int avgMastery = allTopics.isEmpty() ? 0 : (int) allTopics.stream()
                .mapToInt(Topic::getMastery)
                .average().orElse(0);

        long easy = lcSnapshot != null ? lcSnapshot.getEasySolved() : 0;
        long medium = lcSnapshot != null ? lcSnapshot.getMediumSolved() : 0;
        long hard = lcSnapshot != null ? lcSnapshot.getHardSolved() : 0;

        List<AnalyticsResponse.CategoryMastery> categoryMastery = allTopics.stream()
                .collect(Collectors.groupingBy(Topic::getCategory))
                .entrySet().stream()
                .map(entry -> new AnalyticsResponse.CategoryMastery(
                        entry.getKey(),
                        (int) entry.getValue().stream().mapToInt(Topic::getMastery).average().orElse(0)))
                .toList();

        List<AnalyticsResponse.TopicSummary> weakest = weakTopics.stream().limit(5)
                .map(t -> new AnalyticsResponse.TopicSummary(t.getTitle(), t.getConfidence(), t.getMastery(), t.getCategory()))
                .toList();
        List<AnalyticsResponse.TopicSummary> strongest = strongTopics.stream().limit(5)
                .map(t -> new AnalyticsResponse.TopicSummary(t.getTitle(), t.getConfidence(), t.getMastery(), t.getCategory()))
                .toList();

        long streak = calculateStreak(userId);

        User user = userRepository.findById(userId).orElse(null);
        int targetLevel = user != null && user.getTargetLevel() != null ? user.getTargetLevel() : 5;

        int readinessScore = ReadinessCalculator.computeReadinessScore(targetLevel, allTopics, lcSnapshot);

        return new AnalyticsResponse(
                totalProblems,
                totalTopics,
                (double) avgMastery,
                new AnalyticsResponse.DifficultyBreakdown(easy, medium, hard),
                categoryMastery,
                weakest,
                strongest,
                streak,
                targetLevel,
                readinessScore
        );
    }

    public WeeklyProgressResponse getWeeklyProgress() {
        UUID userId = SecurityUtils.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);

        long revisionsCompleted = revisionRepository.countCompletedInRangeByUserId(userId, weekStart, today);
        List<Journal> weekJournals = journalRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, weekStart, today);
        double hoursThisWeek = weekJournals.stream().mapToDouble(j -> j.getHoursStudied() != null ? j.getHoursStudied() : 0).sum();

        return new WeeklyProgressResponse(
                0L,
                0L,
                hoursThisWeek,
                revisionsCompleted,
                weekJournals.size()
        );
    }

    private long calculateStreak(UUID userId) {
        long streak = 0;
        LocalDate date = LocalDate.now();
        while (true) {
            if (journalRepository.findByUserIdAndEntryDate(userId, date).isPresent()) {
                streak++;
                date = date.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
