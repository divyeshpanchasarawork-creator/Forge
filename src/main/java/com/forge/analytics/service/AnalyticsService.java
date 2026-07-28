package com.forge.analytics.service;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.common.util.SecurityUtils;
import com.forge.journal.entity.Journal;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.problem.repository.ProblemRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProblemRepository problemRepository;
    private final TopicRepository topicRepository;
    private final RevisionRepository revisionRepository;
    private final JournalRepository journalRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;

    public AnalyticsResponse getAnalytics() {
        UUID userId = SecurityUtils.getCurrentUserId();

        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        List<Topic> weakTopics = topicRepository.findWeakTopicsByUserId(userId);
        List<Topic> strongTopics = topicRepository.findStrongTopicsByUserId(userId);

        long totalProblems = problemRepository.countByUserId(userId);
        long totalTopics = topicRepository.countByUserId(userId);

        LeetCodeSnapshot lcSnapshot = snapshotRepository.findByUserId(userId).orElse(null);
        if (lcSnapshot != null) {
            totalProblems = lcSnapshot.getTotalSolved();
        }

        Double totalStudyHours = allTopics.stream()
                .mapToDouble(t -> t.getMastery() != null ? t.getMastery() : 0)
                .sum() / 10.0;

        int avgMastery = allTopics.isEmpty() ? 0 : (int) allTopics.stream()
                .mapToInt(Topic::getMastery)
                .average().orElse(0);

        int avgConfidence = allTopics.isEmpty() ? 0 : (int) allTopics.stream()
                .mapToInt(Topic::getConfidence)
                .average().orElse(0);

        long easy;
        long medium;
        long hard;
        if (lcSnapshot != null) {
            easy = lcSnapshot.getEasySolved();
            medium = lcSnapshot.getMediumSolved();
            hard = lcSnapshot.getHardSolved();
        } else {
            easy = problemRepository.countByUserIdAndDifficulty(userId, "EASY");
            medium = problemRepository.countByUserIdAndDifficulty(userId, "MEDIUM");
            hard = problemRepository.countByUserIdAndDifficulty(userId, "HARD");
        }

        List<AnalyticsResponse.CategoryMastery> categoryMastery = allTopics.stream()
                .collect(Collectors.groupingBy(Topic::getCategory))
                .entrySet().stream()
                .map(entry -> new AnalyticsResponse.CategoryMastery(
                        entry.getKey(),
                        (int) entry.getValue().stream().mapToInt(Topic::getMastery).average().orElse(0)))
                .toList();

        List<AnalyticsResponse.LearningTrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(new AnalyticsResponse.LearningTrendPoint(
                    date.format(DateTimeFormatter.ofPattern("MMM dd")),
                    0L,
                    0.0
            ));
        }

        List<AnalyticsResponse.TopicSummary> weakest = weakTopics.stream().limit(5)
                .map(t -> new AnalyticsResponse.TopicSummary(t.getTitle(), t.getConfidence(), t.getMastery(), t.getCategory()))
                .toList();
        List<AnalyticsResponse.TopicSummary> strongest = strongTopics.stream().limit(5)
                .map(t -> new AnalyticsResponse.TopicSummary(t.getTitle(), t.getConfidence(), t.getMastery(), t.getCategory()))
                .toList();

        long totalRevisions = revisionRepository.countByUserIdAndCompleted(userId, true) +
                              revisionRepository.countByUserIdAndCompleted(userId, false);
        long completedRevisions = revisionRepository.countByUserIdAndCompleted(userId, true);
        double completionRate = totalRevisions > 0 ? (double) completedRevisions / totalRevisions * 100 : 0;

        long streak = calculateStreak(userId);
        if (lcSnapshot != null && lcSnapshot.getStreak() != null && lcSnapshot.getStreak() > streak) {
            streak = lcSnapshot.getStreak();
        }

        AnalyticsResponse.LeetCodeOverview lcOverview = null;
        if (lcSnapshot != null) {
            lcOverview = new AnalyticsResponse.LeetCodeOverview(
                    lcSnapshot.getTotalSolved(),
                    lcSnapshot.getEasySolved(),
                    lcSnapshot.getMediumSolved(),
                    lcSnapshot.getHardSolved(),
                    lcSnapshot.getRanking(),
                    lcSnapshot.getStreak(),
                    lcSnapshot.getTotalActiveDays(),
                    lcSnapshot.getEasyBeatsPct(),
                    lcSnapshot.getMediumBeatsPct(),
                    lcSnapshot.getHardBeatsPct()
            );
        }

        return new AnalyticsResponse(
                totalProblems,
                totalTopics,
                totalStudyHours,
                (double) avgMastery,
                avgConfidence,
                new AnalyticsResponse.DifficultyBreakdown(easy, medium, hard),
                categoryMastery,
                completionRate,
                trend,
                weakest,
                strongest,
                streak,
                lcOverview
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
