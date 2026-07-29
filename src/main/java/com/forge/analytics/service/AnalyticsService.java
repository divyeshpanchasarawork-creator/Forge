package com.forge.analytics.service;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

        Double totalStudyHours = allTopics.stream()
                .mapToDouble(t -> t.getMastery() != null ? t.getMastery() : 0)
                .sum() / 10.0;

        int avgMastery = allTopics.isEmpty() ? 0 : (int) allTopics.stream()
                .mapToInt(Topic::getMastery)
                .average().orElse(0);

        int avgConfidence = allTopics.isEmpty() ? 0 : (int) allTopics.stream()
                .mapToInt(Topic::getConfidence)
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

        User user = userRepository.findById(userId).orElse(null);
        int targetLevel = user != null && user.getTargetLevel() != null ? user.getTargetLevel() : 5;

        int readinessScore = computeReadinessScore(targetLevel, allTopics, lcSnapshot);

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
                lcOverview,
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

    private int computeReadinessScore(int targetLevel, List<Topic> allTopics, LeetCodeSnapshot snapshot) {
        int totalSolved = 0;
        int easy = 0;
        int medium = 0;
        int hard = 0;
        if (snapshot != null) {
            totalSolved = snapshot.getTotalSolved() != null ? snapshot.getTotalSolved() : 0;
            easy = snapshot.getEasySolved() != null ? snapshot.getEasySolved() : 0;
            medium = snapshot.getMediumSolved() != null ? snapshot.getMediumSolved() : 0;
            hard = snapshot.getHardSolved() != null ? snapshot.getHardSolved() : 0;
        }

        int targetTotal = getTargetTotal(targetLevel);
        int targetHPct = getTargetHardPct(targetLevel);
        int targetMPct = getTargetMediumPct(targetLevel);
        int targetEPct = getTargetEasyPct(targetLevel);

        int targetHardTotal = (targetHPct * targetTotal) / 100;
        int targetMediumTotal = (targetMPct * targetTotal) / 100;
        int targetEasyTotal = (targetEPct * targetTotal) / 100;

        double problemScore = Math.min(100.0, (double) totalSolved / targetTotal * 100);
        double easyScore = targetEasyTotal > 0 ? Math.min(100.0, (double) easy / targetEasyTotal * 100) : 100.0;
        double mediumScore = targetMediumTotal > 0 ? Math.min(100.0, (double) medium / targetMediumTotal * 100) : 100.0;
        double hardScore = targetHardTotal > 0 ? Math.min(100.0, (double) hard / targetHardTotal * 100) : 100.0;
        double topicScore = allTopics.isEmpty() ? 0 :
                (double) allTopics.stream().filter(t -> t.getConfidence() >= 5).count() / allTopics.size() * 100;

        double readiness = (problemScore * 0.30) + ((easyScore + mediumScore + hardScore) / 3.0 * 0.35) + (topicScore * 0.35);
        return (int) Math.round(Math.min(100, readiness));
    }

    private int getTargetTotal(int level) {
        if (level <= 2) return level <= 1 ? 50 : 80;
        if (level <= 4) return level == 3 ? 120 : 180;
        if (level <= 6) return level == 5 ? 250 : 320;
        if (level <= 8) return level == 7 ? 400 : 500;
        return level == 9 ? 600 : 800;
    }

    private int getTargetHardPct(int level) {
        if (level <= 2) return 0;
        if (level <= 4) return level == 3 ? 10 : 15;
        if (level <= 6) return level == 5 ? 25 : 35;
        if (level <= 8) return level == 7 ? 50 : 60;
        return level == 9 ? 70 : 80;
    }

    private int getTargetMediumPct(int level) {
        if (level <= 2) return level == 1 ? 20 : 30;
        if (level <= 4) return level == 3 ? 40 : 50;
        if (level <= 6) return level == 5 ? 55 : 50;
        if (level <= 8) return level == 7 ? 40 : 35;
        return level == 9 ? 25 : 20;
    }

    private int getTargetEasyPct(int level) {
        return 100 - getTargetHardPct(level) - getTargetMediumPct(level);
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
