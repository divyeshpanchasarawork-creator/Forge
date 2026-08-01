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

        List<Topic> topics = topicRepository.findByUserId(userId, PageRequest.of(0, 1000)).getContent();
        double avgMastery = topics.isEmpty() ? 0 : topics.stream().mapToInt(Topic::getMastery).average().orElse(0);
        double avgConfidence = topics.isEmpty() ? 0 : topics.stream().mapToInt(Topic::getConfidence).average().orElse(0);
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

    @Transactional
    public void snapshotAllUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                snapshotForUser(user.getId());
            } catch (Exception e) {
                log.warn("Snapshot failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    public double computeConsistency(UUID userId) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> activeDays = new HashSet<>();

        dailyMetricRepository.findByUserIdAndMetricDateBetweenOrderByMetricDateAsc(
                        userId, today.minusDays(13), today)
                .forEach(m -> {
                    if (m.getSolvedDelta() != null && m.getSolvedDelta() > 0) activeDays.add(m.getMetricDate());
                    if (m.getRevisionsDone() != null && m.getRevisionsDone() > 0) activeDays.add(m.getMetricDate());
                    if (m.getJournalHours() != null && m.getJournalHours() > 0) activeDays.add(m.getMetricDate());
                });

        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime start = d.atStartOfDay();
            LocalDateTime end = d.atTime(LocalTime.MAX);
            long attempts = problemAttemptRepository.countByUserIdAndAttemptedAtBetween(userId, start, end);
            long revisions = revisionRepository.countCompletedInRangeByUserId(userId, d, d);
            if (attempts > 0 || revisions > 0 || journalRepository.findByUserIdAndEntryDate(userId, d).isPresent()) {
                activeDays.add(d);
            }
        }

        return Math.min(1.0, activeDays.size() / 14.0);
    }
}
