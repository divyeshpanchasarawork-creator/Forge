package com.forge.analytics.service;

import com.forge.analytics.dto.AnalyticsResponse;
import com.forge.analytics.dto.LearningCurveResponse;
import com.forge.analytics.dto.WeeklyProgressResponse;
import com.forge.analytics.entity.DailyMetric;
import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.ReadinessCalculator;
import com.forge.common.util.SecurityUtils;
import com.forge.common.util.TopicFilters;
import com.forge.journal.entity.Journal;
import com.forge.journal.repository.JournalRepository;
import com.forge.leetcode.entity.LeetCodeSnapshot;
import com.forge.leetcode.repository.LeetCodeSnapshotRepository;
import com.forge.practice.entity.ProblemAttempt;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final RevisionRepository revisionRepository;
    private final JournalRepository journalRepository;
    private final LeetCodeSnapshotRepository snapshotRepository;
    private final DailyMetricRepository dailyMetricRepository;
    private final ProblemAttemptRepository problemAttemptRepository;
    private final MetricSnapshotService metricSnapshotService;

    public AnalyticsResponse getAnalytics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        ensureTodaySnapshot(userId);

        List<Topic> allTopics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        List<Topic> weakTopics = topicRepository.findWeakTopicsByUserId(userId);
        List<Topic> strongTopics = topicRepository.findStrongTopicsByUserId(userId);

        long totalTopics = topicRepository.countByUserId(userId);

        LeetCodeSnapshot lcSnapshot = snapshotRepository.findByUserId(userId).orElse(null);
        long totalProblems = lcSnapshot != null ? lcSnapshot.getTotalSolved() : 0;

        int avgMastery = allTopics.isEmpty() ? 0 : (int) allTopics.stream()
                .filter(TopicFilters::isEngaged)
                .mapToInt(Topic::getMastery)
                .average().orElse(0);

        long easy = lcSnapshot != null ? lcSnapshot.getEasySolved() : 0;
        long medium = lcSnapshot != null ? lcSnapshot.getMediumSolved() : 0;
        long hard = lcSnapshot != null ? lcSnapshot.getHardSolved() : 0;

        List<AnalyticsResponse.CategoryMastery> categoryMastery = allTopics.stream()
                .filter(TopicFilters::isEngaged)
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
        List<AnalyticsResponse.Insight> insights = buildInsights(userId, allTopics, lcSnapshot);

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
                readinessScore,
                insights
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

    public LearningCurveResponse getLearningCurve(int days) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ensureTodaySnapshot(userId);
        int window = Math.max(7, Math.min(90, days));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(window - 1L);

        List<DailyMetric> metrics = dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(userId, start, today);
        if (metrics.isEmpty()) {
            return new LearningCurveResponse(List.of(), List.of());
        }

        Map<LocalDate, DailyMetric> byDate = metrics.stream()
                .collect(Collectors.toMap(DailyMetric::getMetricDate, m -> m, (a, b) -> a));

        List<ProblemAttempt> attempts = problemAttemptRepository.findByUserIdAll(userId);

        List<LearningCurveResponse.CurvePoint> points = new ArrayList<>();
        List<LearningCurveResponse.Milestone> milestones = new ArrayList<>();
        List<LocalDate> activeDates = new ArrayList<>();

        double lastMastery = 0, lastConfidence = 0, lastRetention = 100, lastSkill = 1000, lastConsistency = 0;
        boolean started = false;
        boolean crossed50 = false, crossed80 = false;

        for (int i = 0; i < window; i++) {
            LocalDate d = start.plusDays(i);
            DailyMetric metric = byDate.get(d);
            if (metric == null) {
                continue;
            }

            if (!started) {
                lastMastery = metric.getMastery() != null ? metric.getMastery() : 0;
                lastConfidence = metric.getConfidence() != null ? metric.getConfidence() : 0;
                lastRetention = metric.getRetention() != null ? metric.getRetention() : 100;
                lastSkill = metric.getSkillRating() != null ? metric.getSkillRating() : 1000;
                lastConsistency = metric.getConsistency() != null ? metric.getConsistency() : 0;
                started = true;
            }

            double mastery = metric.getMastery() != null ? metric.getMastery() : lastMastery;
            double confidence = metric.getConfidence() != null ? metric.getConfidence() : lastConfidence;
            double retention = metric.getRetention() != null ? metric.getRetention() : lastRetention;
            double skill = metric.getSkillRating() != null ? metric.getSkillRating() : lastSkill;
            double consistency = metric.getConsistency() != null ? metric.getConsistency() : lastConsistency;
            lastMastery = mastery;
            lastConfidence = confidence;
            lastRetention = retention;
            lastSkill = skill;
            lastConsistency = consistency;

            int solved = metric.getSolvedDelta() != null ? metric.getSolvedDelta() : 0;
            int revisions = metric.getRevisionsDone() != null ? metric.getRevisionsDone() : 0;

            if (solved > 0 || revisions > 0 || (metric.getJournalHours() != null && metric.getJournalHours() > 0)) {
                activeDates.add(d);
            }

            List<String> dayMilestones = new ArrayList<>();
            if (!crossed50 && mastery >= 50) {
                crossed50 = true;
                dayMilestones.add("Average mastery crossed 50%");
                milestones.add(new LearningCurveResponse.Milestone(d.toString(), "MASTERY", "Average mastery crossed 50%"));
            }
            if (!crossed80 && mastery >= 80) {
                crossed80 = true;
                dayMilestones.add("Average mastery crossed 80%");
                milestones.add(new LearningCurveResponse.Milestone(d.toString(), "MASTERY", "Average mastery crossed 80%"));
            }
            if (skill >= 1100 && skill - 100 < 1100) {
                dayMilestones.add("Skill rating crossed 1100");
                milestones.add(new LearningCurveResponse.Milestone(d.toString(), "SKILL", "Skill rating crossed 1100"));
            }
            if (skill >= 1400 && skill - 100 < 1400) {
                dayMilestones.add("Skill rating crossed 1400");
                milestones.add(new LearningCurveResponse.Milestone(d.toString(), "SKILL", "Skill rating crossed 1400"));
            }

            points.add(new LearningCurveResponse.CurvePoint(
                    d.toString(), round1(mastery), round1(confidence), round1(retention),
                    round1(skill), round1(consistency * 100), solved, revisions, dayMilestones));
        }

        detectActivityMilestones(userId, attempts, activeDates, milestones);

        return new LearningCurveResponse(points, milestones);
    }

    private void ensureTodaySnapshot(UUID userId) {
        if (dailyMetricRepository.findByUserIdAndMetricDate(userId, LocalDate.now()).isEmpty()) {
            metricSnapshotService.snapshotForUser(userId);
        }
    }

    private void detectActivityMilestones(UUID userId, List<ProblemAttempt> attempts,
                                          List<LocalDate> activeDates, List<LearningCurveResponse.Milestone> milestones) {
        Optional<ProblemAttempt> firstHard = attempts.stream()
                .filter(a -> "HARD".equalsIgnoreCase(a.getDifficulty()) && "SOLVED".equals(a.getOutcome()))
                .min(Comparator.comparing(ProblemAttempt::getAttemptedAt));
        firstHard.ifPresent(a -> milestones.add(new LearningCurveResponse.Milestone(
                a.getAttemptedAt().toLocalDate().toString(), "ACHIEVEMENT", "First Hard problem solved — " + a.getProblemTitle())));

        int streak = 0;
        for (int i = activeDates.size() - 1; i >= 0; i--) {
            if (i > 0 && activeDates.get(i).minusDays(1).equals(activeDates.get(i - 1))) {
                streak++;
            } else if (i == 0) {
                streak++;
            }
            if ((streak == 7 || streak == 14 || streak == 30) && (i == 0 || activeDates.get(i - 1).equals(activeDates.get(i).minusDays(1)))) {
                final int s = streak;
                LocalDate date = activeDates.get(i);
                milestones.add(new LearningCurveResponse.Milestone(date.toString(), "CONSISTENCY", s + "-day active streak"));
            }
        }

        for (int i = 1; i < activeDates.size(); i++) {
            LocalDate prev = activeDates.get(i - 1);
            LocalDate curr = activeDates.get(i);
            if (prev.plusDays(1).isBefore(curr) && java.time.temporal.ChronoUnit.DAYS.between(prev, curr) >= 7) {
                milestones.add(new LearningCurveResponse.Milestone(curr.toString(), "GAP",
                        "Returned after a " + java.time.temporal.ChronoUnit.DAYS.between(prev, curr) + "-day break"));
            }
        }
    }

    private List<AnalyticsResponse.Insight> buildInsights(UUID userId, List<Topic> allTopics, LeetCodeSnapshot snapshot) {
        List<AnalyticsResponse.Insight> insights = new ArrayList<>();
        List<DailyMetric> metrics = dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(
                userId, LocalDate.now().minusDays(30), LocalDate.now());

        if (!metrics.isEmpty()) {
            DailyMetric latest = metrics.get(metrics.size() - 1);
            double masteryNow = latest.getMastery() != null ? latest.getMastery() : 0;
            DailyMetric sevenAgo = metrics.stream()
                    .filter(m -> !m.getMetricDate().isBefore(LocalDate.now().minusDays(8)))
                    .findFirst().orElse(metrics.get(0));
            double masterySevenAgo = sevenAgo.getMastery() != null ? sevenAgo.getMastery() : masteryNow;
            double retentionNow = latest.getRetention() != null ? latest.getRetention() : 100;

            double masteryDelta = round1(masteryNow - masterySevenAgo);
            String masteryMsg;
            if (masteryDelta > 0) {
                masteryMsg = "Mastery is trending up — up " + masteryDelta + "pts over the last week.";
            } else if (masteryDelta < 0) {
                masteryMsg = "Mastery slipped " + Math.abs(masteryDelta) + "pts this week. Schedule a review session.";
            } else {
                masteryMsg = "No change in mastery over the last week.";
            }
            insights.add(new AnalyticsResponse.Insight(
                    "MASTERY", "Mastery Trend", masteryMsg, masteryNow, masteryDelta));

            double skill = latest.getSkillRating() != null ? latest.getSkillRating() : 1000;
            DailyMetric fourteenAgo = metrics.stream()
                    .filter(m -> !m.getMetricDate().isBefore(LocalDate.now().minusDays(15)))
                    .findFirst().orElse(metrics.get(0));
            double skillDelta = round1(skill - (fourteenAgo.getSkillRating() != null ? fourteenAgo.getSkillRating() : skill));
            String skillMsg;
            if (skill <= 0) {
                skillMsg = "Skill rating not computed yet — solve problems at or above your level to calibrate.";
            } else if (skillDelta > 0) {
                skillMsg = "Skill rating " + (int) skill + " — up " + skillDelta + " over two weeks.";
            } else if (skillDelta < 0) {
                skillMsg = "Skill rating " + (int) skill + " — down " + Math.abs(skillDelta) + ". Revisit the basics.";
            } else {
                skillMsg = "No change in skill rating over two weeks.";
            }
            insights.add(new AnalyticsResponse.Insight(
                    "SKILL", "Skill Rating", skillMsg, skill, skillDelta));

            double consistency = latest.getConsistency() != null ? latest.getConsistency() : 0;
            insights.add(new AnalyticsResponse.Insight(
                    "CONSISTENCY", "Consistency",
                    "Active on " + Math.round(consistency * 14) + " of the last 14 days.",
                    round1(consistency * 100), null));
        } else {
            insights.add(new AnalyticsResponse.Insight(
                    "CONSISTENCY", "Consistency", "No daily data yet. Practice or revise today to start your curve.", null, null));
        }

        List<ProblemAttempt> attempts = problemAttemptRepository.findByUserIdAll(userId);
        if (!attempts.isEmpty()) {
            long solved = attempts.stream().filter(a -> "SOLVED".equals(a.getOutcome())).count();
            long partial = attempts.stream().filter(a -> "PARTIAL".equals(a.getOutcome())).count();
            double accuracy = (solved + partial * 0.5) / attempts.size() * 100;
            insights.add(new AnalyticsResponse.Insight(
                    "ACCURACY", "Solve Accuracy", Math.round(accuracy) + "% of tracked attempts resolved (incl. partial).",
                    round1(accuracy), null));
        } else {
            insights.add(new AnalyticsResponse.Insight(
                    "ACCURACY", "Solve Accuracy", "Submit an attempt from the Practice page to unlock accuracy tracking.",
                    null, null));
        }

        long totalProblems = snapshot != null ? snapshot.getTotalSolved() : 0;
        insights.add(new AnalyticsResponse.Insight(
                "PROGRESS", "LeetCode Progress", totalProblems + " problems solved in total.", (double) totalProblems, null));

        return insights;
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

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
