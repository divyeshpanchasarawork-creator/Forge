package com.forge.scheduler;

import com.forge.auth.entity.User;
import com.forge.auth.repository.UserRepository;
import com.forge.common.util.TimezoneUtil;
import com.forge.leetcode.service.LeetCodeFetchService;
import com.forge.recommendation.service.RecommendationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final UserRepository userRepository;
    private final RecommendationEngine recommendationEngine;
    private final LeetCodeFetchService leetCodeFetchService;
    private final SchedulerStatus schedulerStatus;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(cron = "0 */30 * * * *")
    public void runScheduledAnalyses() {
        LocalTime serverNow = LocalTime.now();

        List<User> candidates = userRepository.findByPreferredAnalysisTimeIsNotNull();
        int processed = 0;
        int skippedOutsideWindow = 0;
        int skippedQuota = 0;
        String lastError = null;

        for (User user : candidates) {
            LocalTime preferred = user.getPreferredAnalysisTime();
            if (preferred == null) continue;

            ZoneId zone = TimezoneUtil.resolve(user);
            LocalTime userNow = LocalTime.now(zone);
            log.info("AnalysisScheduler: candidate {} preferred={} zone={} (server {}, local {})",
                    user.getId(), preferred, zone, serverNow, userNow);

            if (!isInWindow(preferred, userNow)) {
                skippedOutsideWindow++;
                continue;
            }

            AtomicBoolean quotaHit = new AtomicBoolean(false);
            try {
                // Quota reset, quota check, generation, and the counter increment run in a single
                // transaction against a freshly-loaded managed entity. The stale detached candidate
                // is never written, so a failed generation cannot burn quota and a concurrent manual
                // generate cannot race the reset.
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.executeWithoutResult(status -> {
                    User managed = userRepository.findById(user.getId())
                            .orElseThrow(() -> new IllegalStateException("User " + user.getId() + " no longer exists"));
                    LocalDate managedToday = LocalDate.now(TimezoneUtil.resolve(managed));
                    if (managed.getLastGenerationDate() == null || !managed.getLastGenerationDate().equals(managedToday)) {
                        managed.setDailyGenerationsUsed(0);
                        managed.setLastGenerationDate(managedToday);
                    }
                    if (managed.getDailyGenerationsUsed() >= 4) {
                        quotaHit.set(true);
                        status.setRollbackOnly();
                        return;
                    }
                    recommendationEngine.generateForUser(managed.getId(), true);
                    leetCodeFetchService.refreshProblemSuggestions(managed.getId());
                    managed.setDailyGenerationsUsed(managed.getDailyGenerationsUsed() + 1);
                    userRepository.save(managed);
                });
            } catch (Exception e) {
                log.error("Scheduled generation failed for user {}: {}", user.getId(), e.getMessage());
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                continue;
            }

            if (quotaHit.get()) {
                skippedQuota++;
            } else {
                processed++;
            }
        }

        schedulerStatus.record(Instant.now(), candidates.size(), processed, skippedOutsideWindow, skippedQuota, lastError);
        log.info("AnalysisScheduler: ran at {} (server) | candidates={} processed={} skipped(outside window)={} skipped(quota)={} error={}",
                serverNow, candidates.size(), processed, skippedOutsideWindow, skippedQuota, lastError == null ? "none" : lastError);
    }

    /**
     * Whether {@code preferred} falls in the half-open 30-minute window around
     * {@code now} (lower bound exclusive, upper bound inclusive). Each 15-minute
     * offset preference therefore fires in exactly one of the two runs that span
     * it, instead of firing in both as a fully-inclusive window would.
     */
    static boolean isInWindow(LocalTime preferred, LocalTime now) {
        LocalTime windowStart = now.minusMinutes(15);
        LocalTime windowEnd = now.plusMinutes(15);
        return preferred.isAfter(windowStart) && !preferred.isAfter(windowEnd);
    }
}
