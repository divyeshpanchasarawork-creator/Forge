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
        LocalTime serverUtc = LocalTime.now(ZoneId.of("UTC"));

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
            LocalDate userToday = LocalDate.now(zone);
            LocalTime windowStart = userNow.minusMinutes(15);
            LocalTime windowEnd = userNow.plusMinutes(15);
            log.info("AnalysisScheduler: candidate {} preferred={} zone={} (server UTC {}, local {} window {}..{})",
                    user.getId(), preferred, zone, serverUtc, userNow, windowStart, windowEnd);

            if (!isInWindow(preferred, userNow)) {
                skippedOutsideWindow++;
                continue;
            }

            if (user.getLastGenerationDate() == null || !user.getLastGenerationDate().equals(userToday)) {
                user.setDailyGenerationsUsed(0);
                user.setLastGenerationDate(userToday);
            }
            if (user.getDailyGenerationsUsed() >= 4) {
                skippedQuota++;
                continue;
            }

            try {
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.executeWithoutResult(status -> {
                    recommendationEngine.generateForUser(user.getId(), true);
                    leetCodeFetchService.refreshProblemSuggestions(user.getId());
                    user.setDailyGenerationsUsed(user.getDailyGenerationsUsed() + 1);
                    userRepository.save(user);
                });
                processed++;
            } catch (Exception e) {
                log.error("Scheduled generation failed for user {}: {}", user.getId(), e.getMessage());
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        schedulerStatus.record(Instant.now(), candidates.size(), processed, skippedOutsideWindow, skippedQuota, lastError);
        log.info("AnalysisScheduler: ran at {} UTC | candidates={} processed={} skipped(outside window)={} skipped(quota)={} error={}",
                serverUtc, candidates.size(), processed, skippedOutsideWindow, skippedQuota, lastError == null ? "none" : lastError);
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
