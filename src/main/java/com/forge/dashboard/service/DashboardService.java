package com.forge.dashboard.service;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.exception.ResourceNotFoundException;
import com.forge.common.util.GreetingUtil;
import com.forge.common.util.ReadinessCalculator;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TimezoneUtil;
import com.forge.dashboard.dto.DashboardResponse;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.recommendation.dto.RecommendationResponse;
import com.forge.recommendation.service.RecommendationService;
import com.forge.revision.dto.RevisionResponse;
import com.forge.revision.service.RevisionService;
import com.forge.topic.dto.TopicResponse;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import com.forge.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final TopicService topicService;
    private final RevisionService revisionService;
    private final RecommendationService recommendationService;
    private final JournalRepository journalRepository;
    private final TopicRepository topicRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String greeting = GreetingUtil.getGreeting(user.getDisplayName(), TimezoneUtil.resolve(user));

        List<TopicResponse> weakTopics = topicService.getWeakTopics();
        String currentFocus = weakTopics.isEmpty() ? "All topics are well covered!" : weakTopics.getFirst().getTitle();

        List<RevisionResponse> revisions = revisionService.getTodayRevisions();
        long revisionsDue = revisions.size();

        String todayMission = String.format("Review %d topics, maintain your streak!", revisionsDue);

        List<RecommendationResponse> recommendations = recommendationService.getActiveRecommendations();

        List<TopicResponse> strongTopics = topicService.getStrongTopics();

        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000));
        int avgMastery = allTopics.isEmpty() ? 0 : (int) allTopics.stream().mapToInt(Topic::getMastery).average().orElse(0);
        int avgConfidence = allTopics.isEmpty() ? 0 : (int) allTopics.stream().mapToInt(Topic::getConfidence).average().orElse(0);
        double avgRetention = allTopics.isEmpty() ? 0.0 : allTopics.stream().mapToDouble(t -> t.getEstimatedRetention() != null ? t.getEstimatedRetention() : 100.0).average().orElse(0.0);
        long masteredCount = allTopics.stream().filter(t -> "MASTERED".equals(t.getStatus())).count();
        long inProgressCount = allTopics.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
        long notStartedCount = allTopics.stream().filter(t -> t.getStatus() == null || "NOT_STARTED".equals(t.getStatus())).count();
        long overdueCount = topicRepository.findTopicsNeedingRevisionByUserId(userId, TimezoneUtil.today(user)).size();

        var todayJournal = journalRepository.findByUserIdAndEntryDate(
                userId, LocalDate.now(TimezoneUtil.resolve(user))).orElse(null);
        String journalSummary = todayJournal != null
                ? "Energy: " + (todayJournal.getEnergy() != null ? todayJournal.getEnergy() + "/5" : "n/a")
                + ", Mood: " + (todayJournal.getMood() != null ? todayJournal.getMood() + "/5" : "n/a")
                : "No journal entry today yet.";

        DashboardResponse.LeetCodeStats lcStats = null;
        LeetCodeSnapshot snapshot = snapshotRepository.findByUserId(userId).orElse(null);
        if (snapshot != null) {
            lcStats = new DashboardResponse.LeetCodeStats(
                    snapshot.getTotalSolved(),
                    snapshot.getEasySolved(),
                    snapshot.getMediumSolved(),
                    snapshot.getHardSolved(),
                    snapshot.getRanking(),
                    snapshot.getStreak(),
                    snapshot.getTotalActiveDays(),
                    snapshot.getContestRating(),
                    snapshot.getContestRanking(),
                    snapshot.getContestAttendedCount(),
                    snapshot.getLastSyncedAt()
            );
        }

        int targetLevel = user.getTargetLevel() != null ? user.getTargetLevel() : 5;
        DashboardResponse.TargetProgress targetProgress = computeTargetProgress(targetLevel, allTopics, snapshot);

        List<DashboardResponse.KnowledgeMapCategory> knowledgeMap = buildKnowledgeMap(allTopics);

        return new DashboardResponse(
                greeting,
                currentFocus,
                todayMission,
                revisions,
                recommendations,
                weakTopics,
                strongTopics,
                new DashboardResponse.KnowledgeHealth(avgMastery, avgConfidence, avgRetention, allTopics.size(), masteredCount, inProgressCount, notStartedCount, overdueCount),
                journalSummary,
                List.of(),
                lcStats,
                targetProgress,
                knowledgeMap
        );
    }

    private DashboardResponse.TargetProgress computeTargetProgress(int targetLevel, List<Topic> allTopics, LeetCodeSnapshot snapshot) {
        int totalSolved = 0;
        int currentEasy = 0;
        int currentMedium = 0;
        int currentHard = 0;

        if (snapshot != null) {
            totalSolved = snapshot.getTotalSolved() != null ? snapshot.getTotalSolved() : 0;
            currentEasy = snapshot.getEasySolved() != null ? snapshot.getEasySolved() : 0;
            currentMedium = snapshot.getMediumSolved() != null ? snapshot.getMediumSolved() : 0;
            currentHard = snapshot.getHardSolved() != null ? snapshot.getHardSolved() : 0;
        }

        int targetTotal = ReadinessCalculator.getTargetTotal(targetLevel);
        int targetHardPct = ReadinessCalculator.getTargetHardPct(targetLevel);
        int targetMediumPct = ReadinessCalculator.getTargetMediumPct(targetLevel);
        int targetEasyPct = ReadinessCalculator.getTargetEasyPct(targetLevel);

        int targetEasy = (targetEasyPct * targetTotal) / 100;
        int targetMedium = (targetMediumPct * targetTotal) / 100;
        int targetHard = (targetHardPct * targetTotal) / 100;

        int readiness = ReadinessCalculator.computeReadinessScore(targetLevel, allTopics, snapshot);

        return new DashboardResponse.TargetProgress(
                targetLevel,
                readiness,
                totalSolved,
                targetTotal,
                new DashboardResponse.DifficultyGap(currentEasy, currentMedium, currentHard, targetEasy, targetMedium, targetHard)
        );
    }

    private List<DashboardResponse.KnowledgeMapCategory> buildKnowledgeMap(List<Topic> allTopics) {
        Map<String, List<Topic>> grouped = allTopics.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(Topic::getCategory, LinkedHashMap::new, Collectors.toList()));

        List<DashboardResponse.KnowledgeMapCategory> result = new ArrayList<>();
        for (Map.Entry<String, List<Topic>> entry : grouped.entrySet()) {
            List<Topic> topics = entry.getValue();
            int avgMastery = (int) topics.stream().mapToInt(Topic::getMastery).average().orElse(0);
            int avgConf = (int) topics.stream().mapToInt(Topic::getConfidence).average().orElse(0);

            List<DashboardResponse.TopicSummary> summaries = topics.stream()
                    .map(t -> new DashboardResponse.TopicSummary(
                            t.getId().toString(),
                            t.getTitle(),
                            t.getMastery(),
                            t.getConfidence(),
                            t.getStatus()))
                    .sorted(Comparator.comparingInt(DashboardResponse.TopicSummary::getMastery))
                    .toList();

            result.add(new DashboardResponse.KnowledgeMapCategory(entry.getKey(), summaries, avgMastery, avgConf));
        }
        return result;
    }

}
