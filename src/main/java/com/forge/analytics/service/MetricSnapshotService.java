package com.forge.analytics.service;

import com.forge.analytics.entity.DailyMetric;
import com.forge.analytics.repository.DailyMetricRepository;
import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.intelligence.service.ForgettingCurveService;
import com.forge.intelligence.service.SkillRatingService;
import com.forge.journal.entity.Journal;
import com.forge.journal.repository.JournalRepository;
import com.forge.practice.repository.ProblemAttemptRepository;
import com.forge.revision.repository.RevisionRepository;
import com.forge.topic.entity.Topic;
import com.forge.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricSnapshotService {

    private final DailyMetricRepository dailyMetricRepository;
    private final TopicRepository topicRepository;
    private final ProblemAttemptRepository problemAttemptRepository;
    private final RevisionRepository revisionRepository;
    private final JournalRepository journalRepository;
    private final ForgettingCurveService forgettingCurveService;
    private final SkillRatingService skillRatingService;
    private final UserRepository userRepository;

    @Transactional
    public DailyMetric snapshotForUser(UUID userId) {
        LocalDate today = LocalDate.now();

        List<Topic> topics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent()
                .stream()
                .filter(com.forge.common.util.TopicFilters::isEngaged)
                .toList();
        double avgMastery = topics.isEmpty() ? 0 : topics.stream()
                .mapToInt(t -> t.getMastery() != null ? t.getMastery() : 0).average().orElse(0);
        double avgConfidence = topics.isEmpty() ? 0 : topics.stream()
                .mapToInt(t -> t.getConfidence() != null ? t.getConfidence() : 0).average().orElse(0);
        double avgRetention = topics.isEmpty() ? 100 : topics.stream()
                .mapToDouble(t -> forgettingCurveService.computeRetention(t, LocalDateTime.now()))
                .average().orElse(100);
        double skillRating = skillRatingService.userSkillFromTopics(topics);

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        long solvedToday = problemAttemptRepository.countByUserIdAndOutcomeAndAttemptedAtBetween(
                userId, "SOLVED", start, end);
        solvedToday += problemAttemptRepository.countByUserIdAndOutcomeAndAttemptedAtBetween(
                userId, "PARTIAL", start, end);
        long revisionsDone = revisionRepository.countCompletedInRangeByUserId(userId, today, today);

        Journal todayJournal = journalRepository.findByUserIdAndEntryDate(userId, today).orElse(null);
        double journalHours = todayJournal != null && todayJournal.getHoursStudied() != null
                ? todayJournal.getHoursStudied() : 0;

        DailyMetric metric = dailyMetricRepository.findByUserIdAndMetricDate(userId, today)
                .orElseGet(() -> {
                    DailyMetric m = new DailyMetric();
                    m.setUser(userRepository.findById(userId).orElseThrow());
                    m.setMetricDate(today);
                    return m;
                });

        metric.setMastery(Math.round(avgMastery * 10) / 10.0);
        metric.setConfidence(Math.round(avgConfidence * 10) / 10.0);
        metric.setRetention(Math.round(avgRetention * 10) / 10.0);
        metric.setSkillRating(Math.round(skillRating * 10) / 10.0);
        metric.setSolvedDelta((int) solvedToday);
        metric.setRevisionsDone((int) revisionsDone);
        metric.setJournalHours(journalHours);
        metric.setConsistency(Math.round(computeConsistency(userId) * 100) / 100.0);

        return dailyMetricRepository.save(metric);
    }

    public double computeConsistency(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(13);
        Set<LocalDate> activeDays = new HashSet<>();

        dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(userId, start, today)
                .forEach(m -> {
                    if (m.getSolvedDelta() != null && m.getSolvedDelta() > 0) activeDays.add(m.getMetricDate());
                    if (m.getRevisionsDone() != null && m.getRevisionsDone() > 0) activeDays.add(m.getMetricDate());
                    if (m.getJournalHours() != null && m.getJournalHours() > 0) activeDays.add(m.getMetricDate());
                });

        problemAttemptRepository.findAttemptedAtInRangeByUserId(
                        userId, start.atStartOfDay(), today.atTime(LocalTime.MAX))
                .forEach(a -> activeDays.add(a.toLocalDate()));

        revisionRepository.findCompletedDatesInRangeByUserId(userId, start, today)
                .forEach(activeDays::add);

        journalRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, start, today)
                .forEach(j -> activeDays.add(j.getEntryDate()));

        return Math.min(1.0, activeDays.size() / 14.0);
    }
}
